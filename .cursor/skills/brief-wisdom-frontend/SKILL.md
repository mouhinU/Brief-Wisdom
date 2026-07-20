---
name: brief-wisdom-frontend
description: >-
  Brief-Wisdom frontend component conventions: HTML template + JS separation,
  component-loader, apiRequest, DOMPurify markdown, i18n, and permission UI.
  Use when editing static JS/HTML/CSS under brief-wisdom-web/src/main/resources/static/.
---

# Brief-Wisdom Frontend

## Stack

- Native HTML / CSS / JavaScript (no React/Vue)
- Component architecture: `.template.js` (HTML) + `.js` (logic)
- Static root: `brief-wisdom-web/src/main/resources/static/`

## Component Structure

Every feature component needs **both** files:

```
components/
├── foo-management.template.js   # HTML generation via render(container)
├── foo-management.js            # IIFE module, business logic
└── component-loader.js          # registerComponent / loadAndInitComponents
```

Reference: [components/README.md](../../../brief-wisdom-web/src/main/resources/static/components/README.md)

## Component JS Template

```javascript
(function () {
    'use strict';

    const API = '/api/xxx';

    async function init(options = {}) {
        const containerId = options.containerId || 'xxx-tab-content';
        const container = document.getElementById(containerId);
        if (container && window.XxxManagementTemplate) {
            XxxManagementTemplate.render(container);
        }
        await loadData();
        setTimeout(applyPermissions, 500);
    }

    async function apiRequest(url, method = 'GET', body = null) {
        const options = { method, headers: { 'Content-Type': 'application/json' } };
        if (body) options.body = JSON.stringify(body);
        const response = await fetch(url, options);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        if (data.success === false) {
            throw new Error(data.error || data.msg || data.message || '请求失败');
        }
        return data;
    }

    window.xxxManagement = { init, /* expose methods for onclick */ };
})();
```

## apiRequest Rules

- **Always** check `response.ok` before `response.json()`
- Check `data.success === false` and surface `data.error` / `data.msg` / `data.message`
- Prefer copying the pattern from `experience-management.js` — do not invent a 5th error-handling style
- Use `credentials: 'include'` when session cookies are required (same-origin fetch includes them by default)

## Page Entry Points

| Page            | Entry JS                  | Tab manager         |
|-----------------|---------------------------|---------------------|
| system-settings | `system-settings-lite.js` | Tab → component map |
| ai-manage       | `ai-manage-lite.js`       | Tab → component map |
| resume-manage   | `resume-manage-lite.js`   | Tab → component map |
| chat            | `chat.js`                 | Standalone          |
| login           | `auth-page.js`            | Standalone          |

New admin tabs: register in the lite JS + add template/component pair.

## XSS & Markdown

Any user/AI-generated HTML **must** go through sanitization:

```javascript
function renderMarkdown(text) {
    const html = marked.parse(text || '');
    if (typeof DOMPurify !== 'undefined') {
        return DOMPurify.sanitize(html, { ADD_ATTR: ['target'] });
    }
    return html;
}
```

- Never assign raw `marked.parse()` to `innerHTML`
- See `chat.js` `renderMarkdown()` as canonical implementation

## Global Dependencies (from navbar.js)

- `showToast(message, type)` — user feedback
- `showConfirmDialog(title, message)` — confirm actions
- `hasPermission(perm)` — button visibility
- `I18n.t(key)` — translations (`js/i18n.js`)

## Permission UI

- Hide action buttons with `data-permission="user:create"` or call `hasPermission()` in JS
- Do not duplicate backend auth — UI hiding is UX only; API enforces real permissions

## i18n

- Static text: `data-i18n="key"` attribute or `I18n.t('key')`
- Add keys to both `i18n/zh-CN.json` and `i18n/en-US.json`

## Modal Pattern

Components use dynamic modal creation:

```javascript
function openModal(title, contentHtml) {
    let modal = document.getElementById('modal');
    if (!modal) { /* create and append to body */ }
    modal.innerHTML = '...' + contentHtml + '...';
    modal.style.display = 'flex';
}
```

Expose `closeModal` on the component's `window.xxxManagement` object.

## CSS

- Page styles: `static/css/{page}.css`
- Component styles may live in parent page CSS
- 4-space indent, no tabs

## Anti-Patterns (Do Not)

- Inline `<script>` business logic in HTML pages — use component JS
- Direct `fetch` without error handling in new code
- Multiple duplicate `apiRequest` implementations with different semantics — align with existing components
- Chinese variable/function names

## New Component Checklist

```
- [ ] Create foo-management.template.js with render(container)
- [ ] Create foo-management.js IIFE with init() + apiRequest
- [ ] Register in component-loader.js or page lite JS
- [ ] Add tab mapping in system-settings-lite.js / ai-manage-lite.js
- [ ] Add permission attributes for admin-only actions
- [ ] Sanitize any dynamic HTML via DOMPurify
```
