# AGENTS.md — ChargeOS-cloud (hcp 微服务系统)

## 定位
基于 Spring Cloud Alibaba 的微服务后端（充电运营系统）。当前仓库只含后端；前端 `CTI-ChargeOS-admin`(Vue2/Element UI) 与小程序 `CTI-ChargeOS-mini`(UniApp) 为独立仓库（README 26-28 行）。

## 怎么跑
- 基础设施：`docker compose -f docker/docker-compose.yml -f docker/docker-compose-infra.yml up -d`（Nacos 8848 / MySQL / Redis / 各微服务 / 网关）。
- 构建（跳过独立的 SB2.7 模块）：`mvn -pl '!hcp-register' package`。
- 网关入口 `:38080`；前端/小程序经 `/prod-api` 前缀由 Nginx 代理到网关（Nginx 去前缀）。
- 密钥外部化：部署前 `cp docker/.env.example docker/.env`，填 `JWT_SECRET`、`NACOS_AUTH_TOKEN` 等（微服务 `bootstrap.yml` 读 `${JWT_SECRET}` 等，缺失则启动失败）。
- `hcp-register` 是独立 Spring Boot 2.7 Nacos 控制台（SB2.7/javax/Java8 二进制），**无法并入 SB3 reactor**，需单独 `mvn -pl hcp-register -am package`；全量构建务必 `-pl '!hcp-register'`。

## 技术栈
Java 17 + Spring Boot 3.2.4 + Spring Cloud 2023.0.1 + Spring Cloud Alibaba 2023.0.1.0 + Nacos 2.3.2(client) + MyBatis-Plus 3.5.7 + Jasypt 3.0.5(配置加密 ENC(...)) + SpringDoc 2.5.0。父 BOM 为独立 parent（非 spring-boot-starter-parent），编译器级别显式 17。

## 目录与约定
- Reactor 模块：hcp-auth(39200) / hcp-gateway(38080) / hcp-visual / hcp-modules(业务微服务) / hcp-api / hcp-common / hcp-demo / hcp-register(SB2.7 独立)。
- 网关聚合 + Nacos 注册发现 + Nacos 配置中心(共享 `application-<env>.yml`，库 `vhcp_config`)。
- 敏感配置用 Jasypt `ENC(...)` 密文，密钥经环境变量注入，禁止明文入库。
- 数据库：`vctgo_platform`(业务)、`vhcp_config`(Nacos 配置)、`seata`。

## 当前状态与下一步
- 分支 `feature/jwt-upgrade-0.11`：JWT 升级与安全/质量修复均已完成并**推送远程**（commit `bad814c`）：JWT_SECRET 外部化 + Nacos server 镜像对齐 2.3.2 + ① 高危：namespace 统一 `public`、Nacos 口令全改 `ENC(...)` + `JasyptEncryptorConfig` 补 `ivGenerator`、`bootstrap.yml` 启用 nacos 账号；② 重要/建议级代码审查修复：`ValidateCodeFilter` 读请求体竞态 NPE 重写（同步聚合+`ServerHttpRequestDecorator` 回写 body）、`CaptchaProperties.enabled` 补默认值、`hcp-register` 插件版本对齐 2.7.18、`chargeServer.address` 改环境变量、`actuator exposure` 收敛、`seata.enabled` 19 处置 false、`hcp-demo` 库名 `vctgo`→`vctgo_platform`、网关白名单破坏性 `- /**` 已删；③ 文档 `AGENTS.md` 同步。
- 已知约束：`hcp-register` 与 SB3 reactor 不兼容，维持现状单独构建。
- 联调验证：JWT 验签链路已用已编译 `JwtUtils` 做 auth↔gateway 跨服务往返测试（错密钥/篡改 token 拒签、缺密钥/弱密钥抛异常均 PASS）；**本地全栈 E2E 已跑通**（原生 MySQL/Redis/Nacos standalone + `java -jar`，无需容器）。后端 `admin/admin123` 登录发 JWT，网关 AuthFilter 验签+redis `login_tokens` 双重校验通过后路由到 hcp-system 业务接口均返回 200。前端 `CTI-ChargeOS-admin`（分支 `fix/frontend-review-issues`）`npm run dev` 起在 80，代理 `/dev-api`→`127.0.0.1:38080` 已验证打通。
- 关键坑（本地联调）：① SB3.2 的 Redis 配置前缀是 `spring.data.redis.*`，**不是** `spring.redis.*`（relaxed binding 不会桥接 `spring.redis→spring.data.redis`）；旧 `spring.redis.*` 在 Docker 因 redis 无密码被掩盖，本地 redis 有密码(`root`)即 `NOAUTH`。已把 Nacos `config_info` 全部 `  redis:`→`  data.redis:` 迁移，并同步回 `sql/hcp_config.sql`（dump+replace）。② 网关验证码开关在 `security.captcha.enabled`（网关 `ValidateCodeFilter`），关闭验证码须用 `--security.captcha.enabled=false` 放在 `-jar` **之后**作为 Spring 参数（放 `-jar` 前会被 JVM 吞掉不生效；`-D` 与 `--` 均低于 Nacos bootstrap 优先级，故改 Nacos/放对位置才有效）。本地 dev 已在 Nacos `hcp-gateway-dev.yml` 置 `false`。③ Nacos 鉴权与账号：本地 dev 服务端鉴权**关闭**（匿名连，`application.properties` `nacos.core.auth.enabled=false`），但 docker 部署经 `NACOS_AUTH_ENABLE=true` 开启鉴权；各模块 `bootstrap.yml` 已启用 nacos `username/password`（`${NACOS_USERNAME:nacos}`/`${NACOS_PASSWORD:nacos123456}`，docker 经 `NACOS_USERNAME/PASSWORD` 覆盖，见 `docker-compose.yml` 各服务 `environment`）。\n④ namespace 统一：全部模块 `bootstrap.yml` 的 `namespace` 已从 `hcp` 改为 `public`（与 `sql/hcp_config.sql` 的 `config_info` 实际所在命名空间一致），启动可去掉 `--spring.cloud.nacos.*.namespace=public` 覆盖（保留无害）。\n⑤ 配置加密（高危已修）：`sql/hcp_config.sql` 与运行库 `config_info` 中的 DB/Redis/邮箱等口令已全部改为 `ENC(...)`（Jasypt `PBEWITHHMACSHA512ANDAES_256`），主密钥经环境变量 `JASYPT_ENCRYPTOR_PASSWORD` 注入（本地 `services.bat` 内联）。**关键坑**：`hcp-common` 的 `JasyptEncryptorConfig.buildEncryptor` 必须显式 `setIvGenerator(RandomIvGenerator)`，否则 AES 算法 jasypt 默认退化空 IV，既无法加密又导致解密失败（已修复并 E2E 验证）。
- 文档已校正（见 README）：运行环境原写 JDK1.8（实为 Java 17）；部署段原写 SCA 2021.0.5.0/nacos 2.1.1（实为 2023.0.1.0/2.3.2）。Nacos server 镜像已从 2.1.1 对齐到 2.3.2（docker/docker-compose.yml、docker-compose-infra.yml、docker/nacos/dockerfile），与微服务 nacos-client 2.3.2 一致，gRPC 端口 9848/9849 不变。
