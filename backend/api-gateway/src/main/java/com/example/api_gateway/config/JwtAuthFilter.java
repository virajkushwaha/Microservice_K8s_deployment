package com.example.api_gateway.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private static final Map<String, List<String>> ROLE_ACCESS = Map.of(
//            "/billing", List.of("RECEPTIONIST"),
            "/email-notification", List.of("RECEPTIONIST", "MANAGER"),
//            "/guest", List.of("RECEPTIONIST", "MANAGER"),
//            "/inventory", List.of("MANAGER"),
            "/payments", List.of("MANAGER", "RECEPTIONIST"),
//            "/report", List.of("OWNER"),
            "/reservations", List.of("MANAGER", "RECEPTIONIST", "OWNER"),
            "/room", List.of("MANAGER", "RECEPTIONIST", "OWNER"),
            "/staff", List.of("MANAGER", "OWNER")
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain){
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        HttpHeaders headers = exchange.getRequest().getHeaders();
        if(path.contains("/auth/login") ||
                (path.contains("/auth/register-owner")) || (path.contains("/swagger-ui/"))){
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if(!jwtUtil.isTokenValid(token)){
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String role = jwtUtil.getRole(token);
        // Role-based path validation
        Optional<String> matchingPrefix = ROLE_ACCESS.keySet().stream()
                .filter(path::startsWith)
                .findFirst();

        if (matchingPrefix.isPresent()) {
            List<String> allowedRoles = ROLE_ACCESS.get(matchingPrefix.get());
            if (!allowedRoles.contains(role)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }
        
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-role", role)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder(){
        return -1;
    }
}

