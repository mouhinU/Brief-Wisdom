package com.mouhin.brief.wisdom.common.menu;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜单视图对象
 */

/**
 * MenuDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class MenuDTO implements Serializable {
    private Long id;
    private Long parentId;
    private String name;
    private String url;
    private String icon;
    private String target;
    private Integer type;
    private String permission;
    private Integer sortOrder;
    private Integer isVisible;
    private Integer requireLogin;
}
