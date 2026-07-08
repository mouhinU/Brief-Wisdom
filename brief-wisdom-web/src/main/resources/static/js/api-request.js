/**
 * Brief-Wisdom 统一 API 请求封装
 * 检查 HTTP 状态码，统一解析 Result 包装与错误信息
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
(function (global) {
    'use strict';

    /**
     * 从响应体中提取错误信息
     */
    function extractErrorMessage(body, fallback) {
        if (!body || typeof body !== 'object') {
            return fallback;
        }
        const data = body.data != null ? body.data : body;
        return body.error || body.msg || body.message
            || data.error || data.msg || data.message
            || fallback;
    }

    /**
     * 统一 API 请求
     *
     * @param {string} url 请求地址
     * @param {string} [method='GET'] HTTP 方法
     * @param {object|null} [body=null] JSON 请求体
     * @param {object} [options={}] 额外 fetch 选项
     * @returns {Promise<any>} 解包后的 data 或原始 JSON
     */
    async function apiRequest(url, method, body, options) {
        const httpMethod = method || 'GET';
        const fetchOptions = Object.assign({
            method: httpMethod,
            credentials: 'same-origin',
            headers: {}
        }, options || {});

        if (!fetchOptions.headers['Content-Type'] && body != null) {
            fetchOptions.headers['Content-Type'] = 'application/json';
        }
        if (body != null && httpMethod !== 'GET' && httpMethod !== 'HEAD') {
            fetchOptions.body = JSON.stringify(body);
        }

        const response = await fetch(url, fetchOptions);
        const contentType = response.headers.get('content-type') || '';
        const isJson = contentType.includes('application/json');
        let payload = null;

        if (isJson) {
            payload = await response.json();
        } else if (!response.ok) {
            const text = await response.text();
            throw new Error(text || `HTTP ${response.status}`);
        }

        if (!response.ok) {
            throw new Error(extractErrorMessage(payload, `HTTP ${response.status}`));
        }

        if (!isJson) {
            return payload;
        }

        if (payload && payload.success === false) {
            throw new Error(extractErrorMessage(payload, '请求失败'));
        }

        return payload && Object.prototype.hasOwnProperty.call(payload, 'data')
            ? payload.data
            : payload;
    }

    global.apiRequest = apiRequest;
    global.BriefWisdomApi = { request: apiRequest };
})(window);
