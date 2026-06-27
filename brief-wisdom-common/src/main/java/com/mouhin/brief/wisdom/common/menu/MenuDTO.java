package com.mouhin.brief.wisdom.common.menu;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜单视图对象
 */
@Data
public class MenuDTO implements Serializable {
    private Long id;
    private String name;
    private String url;
    private String icon;
    private String target;
    private Integer sortOrder;
    private Integer isVisible;
}
