package com.medibook.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;

@Configuration
public class OAuthClientConfig {

    @Bean
    @Conditional(GoogleOAuthConfiguredCondition.class)
    public ClientRegistrationRepository clientRegistrationRepository(AppProperties appProperties) {
        AppProperties.Google google = appProperties.getOauth2().getGoogle();
        ClientRegistration googleRegistration = CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(google.getClientId())
                .clientSecret(google.getClientSecret())
                .scope("openid", "profile", "email")
                .redirectUri(google.getRedirectUri())
                .build();

        return new InMemoryClientRegistrationRepository(googleRegistration);
    }

    @Bean
    @Conditional(GoogleOAuthConfiguredCondition.class)
    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }
}
