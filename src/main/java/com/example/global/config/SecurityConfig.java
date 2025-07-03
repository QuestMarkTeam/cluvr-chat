package com.example.global.config;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


	private final JwtDecoder jwtDecoder;

	public SecurityConfig(
		JwtDecoder jwtDecoder
	) {
		this.jwtDecoder = jwtDecoder;
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {

		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(
		AuthenticationConfiguration configuration
	) throws Exception {
		return configuration.getAuthenticationManager();
	}



	@Bean
	@Order(1)
	public SecurityFilterChain chatChain(HttpSecurity http) throws Exception {
		http
			// CORS 설정: CorsConfigurationSource 빈을 지정
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.securityMatcher("/api/**", "/message")
			.csrf(csrf -> csrf.disable())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.anyRequest().authenticated());
		return http.build();
	}

	// @Bean
	// @Order(2)
	// public SecurityFilterChain defaultChain(HttpSecurity http) throws
	// 	Exception {
	//
	// 	http
	// 		.csrf(csrf -> csrf.disable())
	// 		.formLogin(form -> form.disable())
	// 		.httpBasic(basic -> basic.disable())
	// 		.sessionManagement(sm ->
	// 			sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	// 		)
	// 		.authorizeHttpRequests(auth -> auth
	// 			// 회원가입·로그인만 공개
	// 			.requestMatchers("/api/auth/**", "/my-monitor/**").permitAll()
	// 			// /admin/** 은 ADMIN 권한 필요
	// 			.requestMatchers("/admin/**").hasRole("ADMIN")
	// 			// 그 외 모든 요청은 인증된 사용자여야 함
	// 			.anyRequest().authenticated()
	// 		).oauth2ResourceServer(oauth2 -> oauth2
	// 			.jwt(jwt -> jwt.decoder(jwtDecoder))
	// 		);
	//
	// 	return http.build();
	// }

	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration cfg = new CorsConfiguration();
		cfg.setAllowedOrigins(List.of(allowedOrigins.split(",")));    // 프론트 도메인
		cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
		cfg.setAllowedHeaders(List.of("Content-Type","Authorization"));
		cfg.setAllowCredentials(true);
		cfg.setMaxAge(Duration.ofHours(1));

		UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
		src.registerCorsConfiguration("/**", cfg);
		return src;
	}

}
