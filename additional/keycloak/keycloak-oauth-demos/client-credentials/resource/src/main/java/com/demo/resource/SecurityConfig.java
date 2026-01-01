package com.demo.resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /*
    A SecurityFilterChain is:
    An ordered chain of servlet filters that inspect, authenticate, and authorize every HTTP request.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        /*
        Every endpoint (/, /data, /actuator, everything) requires an authenticated principal.
        If there is no valid authentication, Spring returns 401 Unauthorized
         (or sometimes 403 depending on the exact situation).
         */
        http.authorizeHttpRequests(a -> a
                .anyRequest().authenticated()
        )
                /*
                This enables the OAuth2 Resource Server JWT (JSON Web Token) support:
                Spring looks for an Authorization: Bearer … header
                It parses the token as a JWT
                It verifies it (signature + standard checks like expiry)
                If valid, it creates an authenticated Authentication in the SecurityContext
                */
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        Customizer.withDefaults()
                ));
        return http.build();
    }
}
