package com.hcp.common.swagger.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(SwaggerProperties.class)
@ConditionalOnProperty(name = "swagger.enabled", matchIfMissing = true)
public class SwaggerAutoConfiguration
{
    /**
     * 默认排除路径：Spring Boot 默认错误处理路径与监控端点
     */
    private static final String[] DEFAULT_EXCLUDE_PATH = {"/error", "/actuator/**"};

    @Bean
    public OpenAPI openAPI(SwaggerProperties swaggerProperties)
    {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("Authorization",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")))
                .info(new Info()
                        .title(swaggerProperties.getTitle())
                        .description(swaggerProperties.getDescription())
                        .version(swaggerProperties.getVersion())
                        .contact(new Contact()
                                .name(swaggerProperties.getContactName())
                                .url(swaggerProperties.getContactUrl())
                                .email(swaggerProperties.getContactEmail()))
                        .license(new License()
                                .name(swaggerProperties.getLicense())
                                .url(swaggerProperties.getLicenseUrl())));
    }

    @Bean
    public GroupedOpenApi groupedOpenApi(SwaggerProperties swaggerProperties)
    {
        GroupedOpenApi.Builder builder = GroupedOpenApi.builder()
                .group(StringUtils.hasText(swaggerProperties.getTitle()) ? swaggerProperties.getTitle() : "default");

        if (StringUtils.hasText(swaggerProperties.getBasePackage()))
        {
            builder.packagesToScan(swaggerProperties.getBasePackage());
        }
        else
        {
            builder.pathsToMatch("/**");
        }

        List<String> excludes = new ArrayList<>();
        for (String path : DEFAULT_EXCLUDE_PATH)
        {
            excludes.add(path);
        }
        if (swaggerProperties.getExcludePath() != null)
        {
            excludes.addAll(swaggerProperties.getExcludePath());
        }
        builder.pathsToExclude(excludes.toArray(new String[0]));

        return builder.build();
    }
}
