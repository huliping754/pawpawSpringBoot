package com.petcare.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 全局配置类
 * 配置 API 文档的基本信息、作者、版本等
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置 OpenAPI 文档信息
     * 包含项目标题、描述、版本、作者联系方式等
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("宠物寄养管理后台 API")
                        .description("宠物寄养微信小程序的老板端后台接口文档\n" +
                                "功能包括：\n" +
                                "- 宠物预约、入住、离店管理\n" +
                                "- 成本支出记录\n" +
                                "- 收入统计与报表\n" +
                                "- 系统配置管理\n" +
                                "- Excel 导出功能")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@petcare.com")
                                .url("https://petcare.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
