package com.mouhin.brief.wisdom.ai.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对象存储配置属性
 * <p>
 * 对应 {@code application.yml} 中 {@code app.storage.*} 配置项。
 * {@code type} 决定装配本地磁盘实现（local）还是 MinIO/S3 兼容实现（minio）。
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Data
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * 存储类型：local-本地磁盘，minio-MinIO/S3 兼容对象存储
     */
    private String type = "local";

    /**
     * 本地磁盘存储配置
     */
    private Local local = new Local();

    /**
     * MinIO/S3 兼容存储配置
     */
    private Minio minio = new Minio();

    /**
     * 本地磁盘存储配置项
     */
    @Data
    public static class Local {

        /**
         * 文件存储根目录
         */
        private String baseDir = "./uploads";

        /**
         * 静态资源访问前缀（与 WebMvc 资源映射保持一致）
         */
        private String urlPrefix = "/uploads";
    }

    /**
     * MinIO/S3 兼容存储配置项
     */
    @Data
    public static class Minio {

        /**
         * 服务端点地址
         */
        private String endpoint = "http://localhost:9000";

        /**
         * 访问密钥
         */
        private String accessKey = "";

        /**
         * 私密密钥
         */
        private String secretKey = "";

        /**
         * 存储桶名称
         */
        private String bucket = "brief-wisdom";

        /**
         * 预签名 URL 有效期（秒）
         */
        private int urlExpireSeconds = 3600;
    }
}
