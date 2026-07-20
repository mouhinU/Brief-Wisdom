package com.mouhin.brief.wisdom.common.menu;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单树形视图对象
 */

/**
 * MenuTreeDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class MenuTreeDTO implements Serializable {
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
    private List<MenuTreeDTO> children;
}
