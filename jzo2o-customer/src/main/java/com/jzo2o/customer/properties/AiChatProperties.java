package com.jzo2o.customer.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI聊天配置属性
 *
 * @author itcast
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jzo2o.ai")
public class AiChatProperties {

    /**
     * Ollama API 基础地址，默认 http://localhost:11434
     */
    private String baseUrl = "http://localhost:11434";

    /**
     * 模型名称
     */
    private String model = "qwen3:0.6b";

    /**
     * 连接超时时间（毫秒）
     */
    private Integer timeout = 30000;

    /**
     * 最大token数
     */
    private Integer maxTokens = 1024;

    /**
     * 生成温度
     */
    private Double temperature = 0.7;
}
