package com.mouhin.brief.wisdom.constants;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 配置类
 * <p>
 * 自定义 RedisTemplate 序列化策略 + Spring Cache 缓存管理。
 * <p>
 * Redis Key 统一命名规范（项目前缀 bw:，按业务域划分）：
 * <ul>
 *   <li>bw:menu:{...} — 菜单缓存</li>
 *   <li>bw:user:{...} — 用户/角色/权限缓存</li>
 *   <li>bw:resume:{...} — 简历缓存</li>
 *   <li>bw:ratelimit:{...} — 接口限流计数</li>
 *   <li>bw:lock:{name} — 分布式锁</li>
 *   <li>bw:session:{id} — 用户会话</li>
 * </ul>
 */

/**
 * CachePrefix
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Configuration
@EnableCaching
public class CachePrefix {

    /**
     * 项目全局 Redis Key 前缀
     */
    public static final String KEY_PREFIX = "bw:";


    // ==================== 菜单缓存 ====================
    /**
     * 菜单树（按角色）
     */
    public static final String MENU_TREE_CACHE = "bw:menu:tree";
    /**
     * 公开菜单
     */
    public static final String MENU_PUBLIC_CACHE = "bw:menu:public";
    /**
     * 全部菜单（含隐藏，管理页面用）
     */
    public static final String MENU_ALL_CACHE = "bw:menu:all";

    // ==================== 用户/角色缓存 ====================
    /**
     * 用户角色 Key 列表
     */
    public static final String USER_ROLES_CACHE = "bw:user:roles";
    /**
     * 用户权限标识
     */
    public static final String USER_PERMS_CACHE = "bw:user:perms";
    /**
     * 角色信息
     */
    public static final String USER_ROLE_CACHE = "bw:user:role";
    /**
     * 角色列表（管理页面用）
     */
    public static final String USER_ROLE_LIST_CACHE = "bw:user:role:list";

    // ==================== 简历缓存 ====================
    /**
     * 工作经历列表（含项目、成果、技术栈）
     */
    public static final String RESUME_EXPERIENCES_CACHE = "bw:resume:experiences";

    // ==================== AI缓存 ====================
    /**
     * AI模型列表（含启用/全部/激活）
     */
    public static final String AI_MODEL_CACHE = "bw:ai:model";
    /**
     * AI会话历史列表（管理页面用）
     */
    public static final String AI_SESSION_CACHE = "bw:ai:session";

}
