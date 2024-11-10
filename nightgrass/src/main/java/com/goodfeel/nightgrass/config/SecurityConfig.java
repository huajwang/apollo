package com.goodfeel.nightgrass.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/", "/product/**", "/cart/**",
                                "/login**", "/error",
                                "/images/**", "/css/**", "/webjars/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }

//    // Custom WebFilter to redirect HTTP to HTTPS
//    @Bean
//    public WebFilter httpsRedirectFilter() {
//        return (ServerWebExchange exchange, WebFilterChain chain) -> {
//            if ("http".equals(exchange.getRequest().getURI().getScheme())) {
//                URI httpsUri =
//                        URI.create(exchange.getRequest().getURI().toString().replace("http", "https"));
//                exchange.getResponse().setStatusCode(HttpStatus.PERMANENT_REDIRECT);
//                exchange.getResponse().getHeaders().setLocation(httpsUri);
//                return exchange.getResponse().setComplete();
//            }
//            return chain.filter(exchange);
//        };
//    }

}
