package vn.it4409.g16.user.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("user")
public record UserProperties(
        String jwksUri,
        String issuer,
        String audience,
        List<String> allowedOrigins,
        int maxAddresses,
        String piiKey) {}
