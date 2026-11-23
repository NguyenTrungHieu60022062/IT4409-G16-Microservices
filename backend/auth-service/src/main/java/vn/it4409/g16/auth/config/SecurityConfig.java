package vn.it4409.g16.auth.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
            // CSRF token của Spring tắt vì: (1) endpoint thường dùng Bearer token trong header — trình duyệt
            // không tự gửi header này khi bị trang khác lừa; (2) các endpoint dùng cookie (/refresh, /logout)
            // được bảo vệ bằng SameSite=Strict + kiểm tra Origin (RequestGuardFilter).
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout")
                    .permitAll()
                .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json", "/actuator/health").permitAll()
                .requestMatchers("/error").permitAll()
                // Lớp 1 của RBAC: theo URL. Lớp 2: @PreAuthorize trên từng method của AdminController.
                .requestMatchers(HttpMethod.GET, "/api/admin/roles").hasAuthority("role:read")
                .requestMatchers(HttpMethod.GET, "/api/admin/accounts", "/api/admin/accounts/*").hasAuthority("account:read")
                .requestMatchers(HttpMethod.PUT, "/api/admin/accounts/*/role").hasAuthority("role:assign")
                .requestMatchers(HttpMethod.PUT, "/api/admin/accounts/*/status").hasAuthority("account:lock")
                .requestMatchers("/api/admin/**").denyAll()   // đường admin chưa khai báo -> cấm hẳn
                // Mặc định TỪ CHỐI: endpoint nào quên khai báo cũng cần đăng nhập.
                .anyRequest().authenticated())
            .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
            .headers(h -> h
                // API chỉ trả JSON: không cho nạp bất kỳ tài nguyên nào, không cho nhúng iframe.
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)));
        return http.build();
    }

    /**
     * Claim "roles": ["ADMIN"]           -> authority ROLE_ADMIN     (dùng hasRole('ADMIN'))
     * Claim "perms": ["product:write"]   -> authority product:write  (dùng hasAuthority('product:write'))
     * Nên phân quyền theo permission; role chỉ để hiển thị / trường hợp đặc biệt.
     */
    @Bean
    JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtGrantedAuthoritiesConverter perms = new JwtGrantedAuthoritiesConverter();
        perms.setAuthoritiesClaimName("perms");
        perms.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> all = new ArrayList<>(roles.convert(jwt));
            all.addAll(perms.convert(jwt));
            return all;
        });
        return converter;
    }

    /**
     * Argon2id với tham số OWASP khuyến nghị: m=19 MiB, t=2, p=1.
     * Bọc trong DelegatingPasswordEncoder -> hash có tiền tố "{argon2}", sau này đổi thuật toán/tham số
     * vẫn kiểm được hash cũ và tự băm lại khi người dùng đăng nhập (xem AuthService.login).
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
        return new DelegatingPasswordEncoder("argon2", Map.of("argon2", argon2));
    }

    /**
     * CORS chỉ cho đúng origin của frontend. KHÔNG dùng "*" khi allowCredentials=true
     * (trình duyệt sẽ từ chối, và nếu phản chiếu Origin tuỳ ý thì mọi trang web đều đọc được dữ liệu).
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(AuthProperties props) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(props.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
