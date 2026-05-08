package com.medibook.auth.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class GoogleOAuthConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String clientId = context.getEnvironment().getProperty("app.oauth2.google.client-id");
        String clientSecret = context.getEnvironment().getProperty("app.oauth2.google.client-secret");
        return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
    }
}
