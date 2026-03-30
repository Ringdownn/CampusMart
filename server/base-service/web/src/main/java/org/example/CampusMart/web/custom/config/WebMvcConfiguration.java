package org.example.CampusMart.web.custom.config;


import org.example.CampusMart.web.custom.converter.StringToEnumConverterFactory;
import org.example.CampusMart.web.custom.interceptor.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private StringToEnumConverterFactory stringToEnumConverterFactory;


    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(this.stringToEnumConverterFactory);
    }

    @Autowired
    private AuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.authenticationInterceptor)
                .addPathPatterns("/app/**")
                .excludePathPatterns(
                        "/app/register",
                        "/app/register/**",
                        "/app/login",
                        "/app/login/**"

                );
    }
}
