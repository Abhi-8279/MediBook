package com.medibook.gateway.filter;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestIdGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestIdGatewayFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        String resolvedRequestId = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(REQUEST_ID_HEADER, resolvedRequestId))
                .build();

        ServerWebExchange updatedExchange = exchange.mutate().request(request).build();
        updatedExchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, resolvedRequestId);

        return chain.filter(updatedExchange)
                .doFinally(signalType -> LOGGER.info(
                        "gateway requestId={} method={} path={} status={}",
                        resolvedRequestId,
                        request.getMethod(),
                        request.getURI().getPath(),
                        resolveStatus(updatedExchange.getResponse().getStatusCode())));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String resolveStatus(HttpStatusCode statusCode) {
        return statusCode == null ? "NA" : Integer.toString(statusCode.value());
    }
}
