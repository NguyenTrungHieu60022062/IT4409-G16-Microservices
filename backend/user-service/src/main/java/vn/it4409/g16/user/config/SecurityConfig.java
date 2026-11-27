package vn.it4409.g16.user.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * User Service là một "resource server": nó KHÔNG đăng nhập ai cả, chỉ kiểm access token do Auth
 * Service cấp (theo hợp đồng trong backend/docs/security-contract.md).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
            // Không dùng cookie -> không có CSRF: trình duyệt không tự gắn header Authorization.
            .csrf(c -> c.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .requestMatchers("/error").permitAll()
                // Lớp 1 của RBAC: theo URL, kiểm PERMISSION (không kiểm tên role) -> Auth đổi quyền
                // của role nào thì service này tự theo, không phải sửa code.
                .requestMatchers(HttpMethod.GET, "/api/admin/users").hasAuthority("profile:read:any")
                .requestMatchers("/api/admin/**").denyAll()
                .anyRequest().authenticated())                       // mặc định: phải đăng nhập
            .oauth2ResourceServer(rs -> rs.jwt(j -> j.jwtAuthenticationConverter(jwtAuthConverter())))
            .headers(h -> h
                .contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)));
        return http.build();
    }

    /** Tải public key từ JWKS của Auth Service (có cache), chỉ chấp nhận RS256. */
    @Bean
    JwtDecoder jwtDecoder(UserProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(props.jwksUri())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(validator(props));
        return decoder;
    }

    /** exp/nbf + iss + aud. Dùng chung cho decoder thật và decoder trong test. */
    public static OAuth2TokenValidator<Jwt> validator(UserProperties props) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.issuer()),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(props.audience())));
    }

    /** "roles": ["ADMIN"] -> ROLE_ADMIN ; "perms": ["profile:read:any"] -> profile:read:any */
    @Bean
    JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtGrantedAuthoritiesConverter perms = new JwtGrantedAuthoritiesConverter();
        perms.setAuthoritiesClaimName("perms");
        perms.setAuthorityPrefix("");
        JwtAuthenticationConverter c = new JwtAuthenticationConverter();
        c.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> all = new ArrayList<>(roles.convert(jwt));
            all.addAll(perms.convert(jwt));
            return all;
        });
        return c;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(UserProperties props) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(props.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(false); // service này không dùng cookie
        cors.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
