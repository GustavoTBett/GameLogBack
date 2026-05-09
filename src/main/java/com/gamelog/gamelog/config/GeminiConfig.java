package com.gamelog.gamelog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.gamelog.gamelog.ai.AiClient;
import com.gamelog.gamelog.ai.GoogleGenaiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Bean
    public AiClient aiClient(
            @Value("${ai.gemini.api-key:}") String apiKey,
            @Value("${ai.gemini.model:}") String model
    ) {
        return new GoogleGenaiClient(apiKey, model);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().findAndAddModules().build();
    }
}
