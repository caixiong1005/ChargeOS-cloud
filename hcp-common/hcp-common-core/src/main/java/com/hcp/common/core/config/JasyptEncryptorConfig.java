package com.hcp.common.core.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Jasypt 配置加密。
 *
 * <p>与 Nacos 配置中心天然兼容：Nacos 下发的配置（DB 口令、Redis 口令、各类密钥等）
 * 写成 {@code ENC(...)} 形式后，会在 Spring 解析配置源时由本加密器自动解密。</p>
 *
 * <p>主密钥通过<strong>环境变量</strong> {@code JASYPT_ENCRYPTOR_PASSWORD} 注入（对应属性
 * {@code jasypt.encryptor.password}，绝不落库、不写在配置文件中）。这样密钥独立于代码与配置，
 * 便于统一管理与轮换。</p>
 *
 * <p><b>密钥轮换</b>：解密时依次尝试“当前主密钥”与“历史密钥”（逗号分隔，由环境变量
 * {@code JASYPT_ENCRYPTOR_PASSWORD_PREV} / 属性 {@code jasypt.encryptor.password.prev} 提供）。
 * 轮换步骤：① 注入新主密钥；② 用新密钥重新加密所有 {@code ENC(...)} 并写回 Nacos；
 * ③ 确认全部切换完成后，清空历史密钥。期间旧密文仍可被旧密钥解密，实现无缝过渡。</p>
 *
 * <p>当且仅当 {@code jasypt.encryptor.password} 缺失时本配置不生效，退化为 Jasypt 默认行为，
 * 因此未启用加密的既有部署不受影响。</p>
 */
@Configuration
@ConditionalOnProperty(name = "jasypt.encryptor.password")
public class JasyptEncryptorConfig {

    /** 与 jasypt-maven-plugin 的加密参数保持一致，否则无法互解密。 */
    private static final String ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";
    private static final String ITERATIONS = "1000";
    private static final String POOL_SIZE = "4";
    private static final String PROVIDER = "SunJCE";
    private static final String OUTPUT_TYPE = "base64";

    @Bean(name = "jasyptStringEncryptor")
    public StringEncryptor jasyptStringEncryptor(Environment environment) {
        String primary = environment.getProperty("jasypt.encryptor.password");
        if (primary == null || primary.isBlank()) {
            throw new IllegalStateException(
                    "Jasypt 主密钥缺失：请通过环境变量 JASYPT_ENCRYPTOR_PASSWORD 注入主密钥，"
                            + "切勿将密钥写入配置文件。");
        }

        List<StringEncryptor> previous = new ArrayList<>();
        String prev = environment.getProperty("jasypt.encryptor.password.prev");
        if (prev != null && !prev.isBlank()) {
            for (String key : prev.split(",")) {
                String trimmed = key.trim();
                if (!trimmed.isEmpty()) {
                    previous.add(buildEncryptor(trimmed));
                }
            }
        }
        return new RotatingStringEncryptor(buildEncryptor(primary), previous);
    }

    private PooledPBEStringEncryptor buildEncryptor(String password) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm(ALGORITHM);
        config.setKeyObtentionIterations(ITERATIONS);
        config.setPoolSize(POOL_SIZE);
        config.setProviderName(PROVIDER);
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType(OUTPUT_TYPE);
        encryptor.setConfig(config);
        return encryptor;
    }

    /**
     * 解密时依次尝试主密钥与历史密钥；加密始终使用主密钥。用于密钥轮换过渡期。
     */
    static class RotatingStringEncryptor implements StringEncryptor {

        private final StringEncryptor primary;
        private final List<StringEncryptor> previous;

        RotatingStringEncryptor(StringEncryptor primary, List<StringEncryptor> previous) {
            this.primary = primary;
            this.previous = previous;
        }

        @Override
        public String encrypt(String message) {
            return primary.encrypt(message);
        }

        @Override
        public String decrypt(String encryptedMessage) {
            try {
                return primary.decrypt(encryptedMessage);
            } catch (RuntimeException e) {
                for (StringEncryptor encryptor : previous) {
                    try {
                        return encryptor.decrypt(encryptedMessage);
                    } catch (RuntimeException ignored) {
                        // 尝试下一个历史密钥
                    }
                }
                throw e;
            }
        }
    }
}
