package app.husna.HusnaMainBackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
public class SecurityConfig {

    // H2 console chain (highest priority)
    @Bean
    @Order(1)
    SecurityFilterChain h2ConsoleChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/h2-console/**", "/localhost/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/**","/localhost/**", "/h2-console/**"))
                // allow frames for same-origin so the console works
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                // let H2 create a session if it wants
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }

    // Everything else (dev-open)
    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(req -> {
                    CorsConfiguration c = new CorsConfiguration();
                    c.setAllowedOrigins(List.of("*"));
                    c.setAllowedMethods(List.of("GET","POST","PATCH","PUT","DELETE","OPTIONS"));
                    c.setAllowedHeaders(List.of("*"));
                    return c;
                }))
                // IMPORTANT: also allow frames on the general chain
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(
                                "/localhost/**",
                                "/h2-console/**",                // correct path
                                "/api/test/**",
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/css/**", "/js/**", "/images/**",
                                "/", "/home", "/**"               // UI & dev-open
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
