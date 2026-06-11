package com.its.gestioneordinirestclient.config;

import com.its.gestioneordinirestclient.logging.RequestLoggingInterceptor;
import com.its.gestioneordinirestclient.security.RoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RoleInterceptor roleInterceptor;
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/orders", "/orders/**")
                .excludePathPatterns("/actuator/**")
                .order(1);

        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/orders", "/orders/**")
                .order(2);
    }
}