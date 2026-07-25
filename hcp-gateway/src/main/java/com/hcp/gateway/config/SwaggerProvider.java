package com.hcp.gateway.config;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.context.annotation.Configuration;

/**
 * 基于路由动态聚合各微服务的 OpenAPI 文档（springdoc）。
 * 每个微服务通过 springdoc 暴露 /v3/api-docs，网关在 Swagger UI 下拉框中聚合展示。
 */
@Configuration
@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerProvider
{
    /**
     * springdoc 文档端点（OpenAPI 3）
     */
    public static final String SWAGGER3URL = "/v3/api-docs";

    /**
     * 鉴权服务不需要暴露文档
     */
    private static final String AUTH_SERVICE_ID = "vctgo-auth";

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private GatewayProperties gatewayProperties;

    @Autowired
    private SwaggerUiConfigParameters swaggerUiConfigParameters;

    @PostConstruct
    public void init()
    {
        // 收集当前实际生效的路由 ID
        List<String> routeIds = new ArrayList<>();
        routeLocator.getRoutes().subscribe(route -> routeIds.add(route.getId()));

        // 仅为真实存在且非鉴权的路由生成聚合文档入口
        gatewayProperties.getRoutes().stream()
                .filter(routeDefinition -> routeIds.contains(routeDefinition.getId()))
                .filter(routeDefinition -> !AUTH_SERVICE_ID.equalsIgnoreCase(routeDefinition.getId()))
                .forEach(routeDefinition -> routeDefinition.getPredicates().stream()
                        .filter(predicateDefinition -> "Path".equalsIgnoreCase(predicateDefinition.getName()))
                        .forEach(predicateDefinition -> {
                            String location = predicateDefinition.getArgs()
                                    .get(NameUtils.GENERATED_NAME_PREFIX + "0")
                                    .replace("/**", SWAGGER3URL);
                            swaggerUiConfigParameters.getUrls()
                                    .add(new AbstractSwaggerUiConfigProperties.SwaggerUrl(location, routeDefinition.getId(), routeDefinition.getId()));
                        }));
    }
}
