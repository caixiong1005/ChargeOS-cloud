package com.hcp.gateway.filter;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hcp.common.core.utils.ServletUtils;
import com.hcp.common.core.utils.StringUtils;
import com.hcp.gateway.config.properties.CaptchaProperties;
import com.hcp.gateway.service.ValidateCodeService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 验证码过滤器
 *
 * @author vctgo
 */
@Component
public class ValidateCodeFilter extends AbstractGatewayFilterFactory<Object>
{
    private final static String[] VALIDATE_URL = new String[] { "/auth/login", "/auth/register" };

    @Autowired
    private ValidateCodeService validateCodeService;

    @Autowired
    private CaptchaProperties captchaProperties;

    private static final String CODE = "code";

    private static final String UUID = "uuid";

    @Override
    public GatewayFilter apply(Object config)
    {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 非登录/注册请求或验证码关闭，不处理
            if (!StringUtils.equalsAnyIgnoreCase(request.getURI().getPath(), VALIDATE_URL) || !captchaProperties.getEnabled())
            {
                return chain.filter(exchange);
            }

            // 同步聚合完整请求体，避免异步 subscribe 竞态（回调未执行即返回 null 导致 NPE）；
            // 读完后通过装饰器重新提供请求体，保证下游 login 接口仍能读取到原始报文。
            return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    String bodyStr = new String(bytes, StandardCharsets.UTF_8);
                    try
                    {
                        JSONObject obj = JSON.parseObject(bodyStr);
                        validateCodeService.checkCaptcha(obj.getString(CODE), obj.getString(UUID));
                    }
                    catch (Exception e)
                    {
                        return ServletUtils.webFluxResponseWriter(exchange.getResponse(), e.getMessage());
                    }
                    final byte[] bodyBytes = bytes;
                    ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(request)
                    {
                        @Override
                        public Flux<DataBuffer> getBody()
                        {
                            return Flux.just(exchange.getResponse().bufferFactory().wrap(bodyBytes));
                        }

                        @Override
                        public HttpHeaders getHeaders()
                        {
                            HttpHeaders headers = new HttpHeaders();
                            headers.putAll(super.getHeaders());
                            // 重新写入了请求体，移除原 content-length 改用 chunked，避免长度不一致
                            headers.remove(HttpHeaders.CONTENT_LENGTH);
                            headers.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                            return headers;
                        }
                    };
                    return chain.filter(exchange.mutate().request(decorator).build());
                });
        };
    }
}
