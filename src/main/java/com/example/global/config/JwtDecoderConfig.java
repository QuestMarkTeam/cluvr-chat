package com.example.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {

	@Bean
	public JwtDecoder jwtDecoder(@Value("${cognito.jwkSetUri}") String jwkSetUri) {
		return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
	}
}
