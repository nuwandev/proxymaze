package com.binarybeasts.config;

import com.binarybeasts.domain.RuntimeConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyMazeConfig {

    @Bean
    public RuntimeConfig runtimeConfig() {
        return new RuntimeConfig();
    }
}
