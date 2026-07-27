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
- 分支 `feature/jwt-upgrade-0.11`：JWT 升级已完成并**推送远程**（JWT_SECRET 外部化、密钥改由环境变量注入；含 Nacos server 镜像对齐 2.3.2）。工作树干净，与 origin 同步。
- 已知约束：`hcp-register` 与 SB3 reactor 不兼容，维持现状单独构建。
- 联调验证：JWT 验签链路已用已编译 `JwtUtils` 做 auth↔gateway 跨服务往返测试（错密钥/篡改 token 拒签、缺密钥/弱密钥抛异常均 PASS）；**全栈 E2E 登录联调未跑通**——本机 Docker Desktop 引擎 500 + 需登录账号（注册不了），阻塞基础设施。待选免账号容器运行时（Rancher Desktop / Podman Desktop）或原生 MySQL/Redis/Nacos standalone 后继续；之后同步前端/小程序对接。
- 文档已校正（见 README）：运行环境原写 JDK1.8（实为 Java 17）；部署段原写 SCA 2021.0.5.0/nacos 2.1.1（实为 2023.0.1.0/2.3.2）。Nacos server 镜像已从 2.1.1 对齐到 2.3.2（docker/docker-compose.yml、docker-compose-infra.yml、docker/nacos/dockerfile），与微服务 nacos-client 2.3.2 一致，gRPC 端口 9848/9849 不变。
