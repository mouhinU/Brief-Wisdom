package com.mouhin.brief.wisdom.persistence.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类
 */

/**
 * MybatisPlusConfig
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Configuration
public class MybatisPlusConfig implements MetaObjectHandler {

    /**
     * 配置分页插件和逻辑删除
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(1000L);  // 最大单页限制数量
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // timestamp 字段：如果已经手动设置了值，则不覆盖
        Object timestamp = this.getFieldValByName("timestamp", metaObject);
        if (timestamp == null) {
            this.strictInsertFill(metaObject, "timestamp", LocalDateTime.class, LocalDateTime.now());
        }
        // 设置默认的逻辑删除字段为 0（未删除）
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
    }

    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
