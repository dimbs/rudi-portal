package org.rudi.tools.nodestub.config.security;

import java.util.Arrays;

import javax.servlet.Filter;

import org.rudi.common.facade.config.filter.JwtRequestFilter;
import org.rudi.common.facade.config.filter.OAuth2RequestFilter;
import org.rudi.common.facade.config.filter.PreAuthenticationFilter;
import org.rudi.common.service.helper.UtilContextHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class WebSecurityConfig {

	private static final String[] SB_PERMIT_ALL_URL = {
			// swagger ui / openapi
			"/nodestub/v3/api-docs/**", "/nodestub/swagger-ui/**", "/nodestub/swagger-ui.html",
			"/nodestub/swagger-resources/**", "/configuration/ui", "/configuration/security", "/webjars/**",
			"/nodestub/endpoints/**", "nodestub/test/**" };

	@Value("${module.oauth2.check-token-uri}")
	private String checkTokenUri;

	private boolean disableAuthentification = false;

	private final UtilContextHelper utilContextHelper;
	private final RestTemplate oAuth2RestTemplate;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		if (!disableAuthentification) {
			http.cors().and().csrf().disable()
					// starts authorizing configurations
					.authorizeRequests().antMatchers(SB_PERMIT_ALL_URL).permitAll()
					// configuring the session on the server
					.and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
					// installation du filtre de type header
					.and().addFilterBefore(createOAuth2Filter(), UsernamePasswordAuthenticationFilter.class)
					.addFilterBefore(createJwtRequestFilter(), UsernamePasswordAuthenticationFilter.class)
					.addFilterAfter(createPreAuthenticationFilter(), BasicAuthenticationFilter.class);
		} else {
			http.cors().and().csrf().disable().authorizeRequests().anyRequest().permitAll();
		}
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		final CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS", "PUT", "DELETE"));
		configuration.addAllowedHeader("*");
		configuration.addExposedHeader("Authorization");
		configuration.addExposedHeader("X-TOKEN");
		configuration.setAllowCredentials(true);

		// Url autorisées
		// 4200 pour les développement | 8080 pour le déploiement
		configuration.setAllowedOriginPatterns(Arrays.asList("*"));

		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public JwtRequestFilter createJwtRequestFilter() {
		return new JwtRequestFilter(SB_PERMIT_ALL_URL, utilContextHelper, oAuth2RestTemplate);
	}

	private Filter createOAuth2Filter() {
		return new OAuth2RequestFilter(SB_PERMIT_ALL_URL, checkTokenUri, utilContextHelper, oAuth2RestTemplate);
	}

	private Filter createPreAuthenticationFilter() {
		return new PreAuthenticationFilter();
	}

}
