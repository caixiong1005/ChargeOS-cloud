package com.hcp.common.core.config;

import brave.Span;
import brave.propagation.CurrentTraceContext;
import brave.Tracing;
import brave.propagation.TraceContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 链路追踪：OpenFeign 调用方上下文传播。
 *
 * <p>服务端链路由 Micrometer Tracing（Brave 桥接）自动注册的 {@code TracingFilter} 完成；
 * 此处补齐 Feign 客户端在发起远程调用时的 trace 上下文注入，使 traceId 跨服务透传，
 * 形成完整调用链。仅依赖 Brave 原生 API，避免引入与 OpenFeign 版本耦合的额外组件。</p>
 */
@Configuration
@ConditionalOnClass(name = "io.micrometer.tracing.Tracer")
public class TracingConfig {

    @Bean
    public RequestInterceptor braveTracingRequestInterceptor(Tracing tracing) {
        TraceContext.Injector<RequestTemplate> injector =
                tracing.propagation().injector((carrier, key, value) -> carrier.header(key, value));
        return template -> {
            Span span = tracing.tracer()
                    .nextSpan()
                    .name(template.method())
                    .kind(Span.Kind.CLIENT)
                    .start();
            try (CurrentTraceContext.Scope scope = tracing.currentTraceContext().newScope(span.context())) {
                injector.inject(span.context(), template);
            } finally {
                span.finish();
            }
        };
    }
}
