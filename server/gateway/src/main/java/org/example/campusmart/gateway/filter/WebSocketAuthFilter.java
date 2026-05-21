package org.example.campusmart.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class WebSocketAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (!path.startsWith("/ws")) {
            return chain.filter(exchange);
        }

        String token = request.getQueryParams().getFirst("token");
        
        if (token == null || token.isEmpty()) {
            token = request.getHeaders().getFirst("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
        }

        if (token == null || token.isEmpty()) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String userId = getUserId(claims);
            String username = claims.get("username", String.class);

            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Name", Objects.toString(username, ""))
                .header("access-token", token)
                .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -99;
    }

    private String getUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId != null) {
            return userId.toString();
        }
        return claims.getSubject();
    }
}
