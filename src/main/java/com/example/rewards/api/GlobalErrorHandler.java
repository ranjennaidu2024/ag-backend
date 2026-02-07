package com.example.rewards.api;

import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

@Component
@Primary
@Order(-2)
public class GlobalErrorHandler implements WebExceptionHandler {

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    String payload = "{\"error\":\"" + ex.getMessage() + "\"}";
    var buffer = exchange.getResponse().bufferFactory().wrap(payload.getBytes());
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }
}

