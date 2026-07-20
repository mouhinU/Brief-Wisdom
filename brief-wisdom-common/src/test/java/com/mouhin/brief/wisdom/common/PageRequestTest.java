package com.mouhin.brief.wisdom.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PageRequest 分页请求参数测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@DisplayName("PageRequest 分页请求参数测试")
class PageRequestTest {

    @Test
    @DisplayName("默认值应为 page=1, size=20")
    void testDefaults() {
        PageRequest req = new PageRequest();

        assertEquals(1, req.getPage());
        assertEquals(20, req.getSize());
    }

    @Test
    @DisplayName("offset() 应正确计算偏移量")
    void testOffset() {
        PageRequest req = new PageRequest();
        req.setPage(1);
        req.setSize(10);

        assertEquals(0, req.offset());
    }

    @Test
    @DisplayName("offset() 第二页应返回正确的偏移量")
    void testOffsetSecondPage() {
        PageRequest req = new PageRequest();
        req.setPage(3);
        req.setSize(20);

        assertEquals(40, req.offset());
    }

    @Test
    @DisplayName("setter 应正确设置 page 和 size")
    void testSetters() {
        PageRequest req = new PageRequest();
        req.setPage(5);
        req.setSize(50);

        assertEquals(5, req.getPage());
        assertEquals(50, req.getSize());
    }
}
