package com.hcp.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * 验证码配置
 *
 * @author vctgo
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "security.captcha")
@Data
public class CaptchaProperties
{
    /**
     * 验证码开关（缺省默认开启，避免配置缺失时 !null 触发 NPE 且保持安全默认）
     */
    private Boolean enabled = true;

    /**
     * 验证码类型（math 数组计算 char 字符）
     */
    private String type;
}
