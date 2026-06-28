package com.mouhin.brief.wisdom.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 分页配置属性 - 按业务维度配置每页查询条数
 * <p>
 * 在 application.yml 中配置：
 * <pre>
 * app:
 *   pagination:
 *     session-list:
 *       default-size: 20
 *       max-size: 100
 *     message-history:
 *       default-size: 20
 *       max-size: 200
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationProperties {

    /**
     * 会话列表分页配置
     */
    private PageConfig sessionList = new PageConfig(20, 100);

    /**
     * 消息历史分页配置
     */
    private PageConfig messageHistory = new PageConfig(20, 200);

    /**
     * 单个业务的分页配置
     */
    @Data
    public static class PageConfig {
        /**
         * 默认每页条数
         */
        private int defaultSize = 20;
        /**
         * 最大每页条数（防止一次性拉取过多数据）
         */
        private int maxSize = 100;

        public PageConfig() {
        }

        public PageConfig(int defaultSize, int maxSize) {
            this.defaultSize = defaultSize;
            this.maxSize = maxSize;
        }

        /**
         * 校验并修正前端传入的 size 参数
         * - 若未传（null 或 <=0），使用 defaultSize
         * - 若超过 maxSize，截断为 maxSize
         *
         * @param requestedSize 前端请求的 size
         * @return 修正后的合法 size
         */
        public int resolveSize(Integer requestedSize) {
            if (requestedSize == null || requestedSize <= 0) {
                return defaultSize;
            }
            return Math.min(requestedSize, maxSize);
        }
    }
}
