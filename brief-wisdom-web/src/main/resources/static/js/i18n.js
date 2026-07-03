/**
 * 前端国际化框架 - 轻量级 i18n 工具
 *
 * 使用方式：
 *   I18n.t('common.save')          → 获取翻译
 *   I18n.t('common.save', 'Save')  → 获取翻译，缺省返回默认值
 *   I18n.setLocale('en-US')        → 切换语言
 *   I18n.getLocale()               → 获取当前语言
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    // 当前语言
    let currentLocale = 'zh-CN';

    // 翻译缓存 { 'zh-CN': {...}, 'en-US': {...} }
    const translations = {};

    // 已加载的语言集合
    const loadedLocales = new Set();

    // 支持的语言列表
    const supportedLocales = ['zh-CN', 'en-US'];

    /**
     * 初始化 i18n
     * 从 Cookie 或浏览器语言检测当前语言
     */
    async function init() {
        // 1. 从 Cookie 读取用户选择的语言
        const cookieLocale = getCookie('brief_wisdom_locale');
        if (cookieLocale && supportedLocales.includes(cookieLocale)) {
            currentLocale = cookieLocale;
        } else {
            // 2. 从浏览器语言检测
            const browserLang = navigator.language || navigator.userLanguage || 'zh-CN';
            if (supportedLocales.includes(browserLang)) {
                currentLocale = browserLang;
            } else if (browserLang.startsWith('zh')) {
                currentLocale = 'zh-CN';
            } else {
                currentLocale = 'en-US';
            }
        }

        // 3. 加载当前语言的翻译文件
        await loadLocale(currentLocale);

        // 4. 预加载另一种语言（加速切换）
        for (const locale of supportedLocales) {
            if (locale !== currentLocale) {
                loadLocale(locale); // 不 await，异步加载
            }
        }

        console.log('[I18n] 初始化完成, locale=' + currentLocale);
    }

    /**
     * 加载指定语言的翻译文件
     */
    async function loadLocale(locale) {
        if (loadedLocales.has(locale)) return;

        try {
            const response = await fetch('/i18n/' + locale + '.json?v=' + Date.now());
            if (!response.ok) throw new Error('HTTP ' + response.status);
            translations[locale] = await response.json();
            loadedLocales.add(locale);
            console.log('[I18n] 翻译文件加载成功: ' + locale);
        } catch (error) {
            console.warn('[I18n] 翻译文件加载失败: ' + locale, error);
            if (!translations[locale]) {
                translations[locale] = {};
            }
        }
    }

    /**
     * 获取翻译文本
     * @param {string} key - 翻译 key
     * @param {string} [defaultValue] - 缺省值（默认返回 key 本身）
     * @returns {string} 翻译文本
     */
    function t(key, defaultValue) {
        const dict = translations[currentLocale] || {};
        const value = dict[key];
        if (value !== undefined && value !== '') return value;

        // 回退到 zh-CN
        if (currentLocale !== 'zh-CN') {
            const zhDict = translations['zh-CN'] || {};
            const zhValue = zhDict[key];
            if (zhValue !== undefined && zhValue !== '') return zhValue;
        }

        return defaultValue !== undefined ? defaultValue : key;
    }

    /**
     * 切换语言
     * @param {string} locale - 目标语言
     */
    async function setLocale(locale) {
        if (!supportedLocales.includes(locale)) {
            console.warn('[I18n] 不支持的语言: ' + locale);
            return;
        }

        await loadLocale(locale);
        currentLocale = locale;

        // 写入 Cookie（365天有效）
        setCookie('brief_wisdom_locale', locale, 365);

        // 更新 <html lang>
        document.documentElement.lang = locale;

        // 触发语言切换事件，让各组件重新渲染
        window.dispatchEvent(new CustomEvent('i18n:localeChanged', { detail: { locale } }));

        console.log('[I18n] 语言切换为: ' + locale);
    }

    /**
     * 获取当前语言
     */
    function getLocale() {
        return currentLocale;
    }

    /**
     * 获取支持的语言列表
     */
    function getSupportedLocales() {
        return [...supportedLocales];
    }

    /**
     * 批量翻译 DOM 中的 data-i18n 属性
     * <span data-i18n="common.save"></span> → 自动替换文本内容
     * <input data-i18n-placeholder="common.search"> → 替换 placeholder
     * <img data-i18n-alt="common.logo"> → 替换 alt
     */
    function translateDOM(root) {
        const container = root || document;

        // data-i18n → textContent
        container.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            const text = t(key);
            if (text !== key) el.textContent = text;
        });

        // data-i18n-placeholder → placeholder
        container.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
            const key = el.getAttribute('data-i18n-placeholder');
            const text = t(key);
            if (text !== key) el.placeholder = text;
        });

        // data-i18n-title → title
        container.querySelectorAll('[data-i18n-title]').forEach(el => {
            const key = el.getAttribute('data-i18n-title');
            const text = t(key);
            if (text !== key) el.title = text;
        });

        // data-i18n-alt → alt
        container.querySelectorAll('[data-i18n-alt]').forEach(el => {
            const key = el.getAttribute('data-i18n-alt');
            const text = t(key);
            if (text !== key) el.alt = text;
        });
    }

    // ===== Cookie 工具 =====

    function getCookie(name) {
        const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
        return match ? decodeURIComponent(match[2]) : null;
    }

    function setCookie(name, value, days) {
        const expires = new Date(Date.now() + days * 864e5).toUTCString();
        document.cookie = name + '=' + encodeURIComponent(value) + ';expires=' + expires + ';path=/;SameSite=Lax';
    }

    // 导出命名空间
    window.I18n = {
        init,
        t,
        setLocale,
        getLocale,
        getSupportedLocales,
        translateDOM,
        loadLocale
    };

    console.log('[I18n] 国际化框架加载成功');
})();
