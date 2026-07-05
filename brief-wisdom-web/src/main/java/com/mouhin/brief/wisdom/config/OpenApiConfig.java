package com.mouhin.brief.wisdom.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 文档配置
 * <p>
 * 集成 SpringDoc OpenAPI 3，自动生成 Swagger UI 文档。
 * 访问地址：/swagger-ui.html 或 /v3/api-docs
 *
 * @author Brief-Wisdom
 * @date 2026-07-05
 */
@Configuration
public class OpenApiConfig {

    /**
     * 自定义 OpenAPI 元信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Brief-Wisdom API")
                        .version("1.0.0")
                        .description("Brief-Wisdom 智能助手平台 RESTful API 文档")
                        .contact(new Contact()
                                .name("Brief-Wisdom")
                        )
                )
                .tags(List.of(
                        new Tag().name("AI对话").description("AI 智能对话相关接口"),
                        new Tag().name("AI管理").description("AI 模型管理、审计、会话历史管理"),
                        new Tag().name("知识库").description("知识库管理与 RAG 检索"),
                        new Tag().name("简历展示").description("简历公开展示接口"),
                        new Tag().name("简历管理").description("简历数据 CRUD 管理"),
                        new Tag().name("认证授权").description("用户登录、注册、OAuth 回调"),
                        new Tag().name("用户管理").description("用户信息查询与管理"),
                        new Tag().name("菜单管理").description("系统菜单管理"),
                        new Tag().name("角色管理").description("角色权限管理")
                ));
    }
}
