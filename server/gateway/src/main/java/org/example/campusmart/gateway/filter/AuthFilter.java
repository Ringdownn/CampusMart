package org.example.campusmart.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final List<String> WHITE_LIST = List.of(
        "/app/login",
        "/app/register",
        "/app/goods/page",
        "/app/goods/search",
        "/app/goods/selectById",
        "/app/messages/list",
        "/app/messages/recent",
        "/app/payments/alipay/notify"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (path.startsWith("/ws") || path.startsWith("/api/") || isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String token = request.getHeaders().getFirst("Authorization");
        String accessToken = request.getHeaders().getFirst("access-token");
        
        String jwt = null;
        
        if (token != null && token.startsWith("Bearer ")) {
            jwt = token.substring(7);
        } else if (accessToken != null) {
            jwt = accessToken;
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret.getBytes())
                .build()
                .parseClaimsJws(jwt)
                .getBody();
            
            String userId = getUserId(claims);
            String username = claims.get("username", String.class);
            
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Name", Objects.toString(username, ""))
                .header("access-token", jwt)
                .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(whitePath -> path.equals(whitePath) || path.startsWith(whitePath + "/"));
    }

    private String getUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId != null) {
            return userId.toString();
        }
        return claims.getSubject();
    }
}
