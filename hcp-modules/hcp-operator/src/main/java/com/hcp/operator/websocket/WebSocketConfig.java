package com.hcp.operator.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSocketConfig {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    /**
     * 原实现声明了 ServerEndpointExporter 用于注册 JSR-356 @ServerEndpoint
     * （/websocket/charge/{orderNumber} 充电订单实时推送）。
     *
     * 但 ServerEndpointExporter 依赖 servlet 容器提供的 jakarta.websocket.server.ServerContainer
     * （由 Tomcat 的 WsSci 在上下文初始化时注册进 servlet context）。
     * 当前嵌入式 Tomcat 部署环境下该 ServerContainer 未被注册，实例化 ServerEndpointExporter 会在
     * afterPropertiesSet 抛 "jakarta.websocket.server.ServerContainer not available"，直接拖垮整个
     * operator 启动，导致 /operator/** 全部 503（首页 dashboard 的充电/运营统计数据拿不到）。
     *
     * 权衡：仪表盘走的是 REST 接口，WebSocket 实时推送是次要能力。故此处暂不声明 ServerEndpointExporter，
     * 保证 operator 正常启动、REST 接口可用；WebSocket 实时推送降级（如需启用，需先解决嵌入式 Tomcat
     * 的 JSR-356 ServerContainer 注册问题，例如补充对应的 websocket SCI 配置）。
     */
    static {
        log.info("WebSocket ServerEndpointExporter 未声明：WebSocket 实时推送降级，REST 接口正常。");
    }
}
