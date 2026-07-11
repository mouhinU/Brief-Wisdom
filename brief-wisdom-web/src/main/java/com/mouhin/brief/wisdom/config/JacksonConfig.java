package com.mouhin.brief.wisdom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 2.x ObjectMapper 配置
 * <p>
 * Spring Boot 4.0.7 默认自动配置 Jackson 3.x（tools.jackson.databind.json.JsonMapper），
 * 但项目中部分代码仍使用 Jackson 2.x（com.fasterxml.jackson.databind.ObjectMapper）。
 * 此配置手动注册一个 Jackson 2.x 的 ObjectMapper Bean 以保持兼容。
 *
 * @author Brief-Wisdom
 * @date 2026-07-12
 */
@Configuration
public class JacksonConfig {

    /**
     * Jackson 2.x ObjectMapper（供项目代码使用）
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
