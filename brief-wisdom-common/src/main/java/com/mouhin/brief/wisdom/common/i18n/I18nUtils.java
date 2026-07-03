package com.mouhin.brief.wisdom.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化工具类 - 获取国际化消息
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
@Component
public class I18nUtils {

    private final MessageSource messageSource;

    public I18nUtils(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 获取国际化消息（使用当前请求的 Locale）
     *
     * @param code 消息代码
     * @return 国际化消息
     */
    public String getMessage(String code) {
        return getMessage(code, null, code);
    }

    /**
     * 获取国际化消息（使用当前请求的 Locale，带参数）
     *
     * @param code 消息代码
     * @param args 参数数组
     * @return 国际化消息
     */
    public String getMessage(String code, Object[] args) {
        return getMessage(code, args, code);
    }

    /**
     * 获取国际化消息（指定默认值）
     *
     * @param code         消息代码
     * @param args         参数数组
     * @param defaultMessage 默认消息
     * @return 国际化消息
     */
    public String getMessage(String code, Object[] args, String defaultMessage) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }

    /**
     * 获取国际化消息（指定 Locale）
     *
     * @param code   消息代码
     * @param locale 语言区域
     * @return 国际化消息
     */
    public String getMessage(String code, Locale locale) {
        return messageSource.getMessage(code, null, code, locale);
    }
}
