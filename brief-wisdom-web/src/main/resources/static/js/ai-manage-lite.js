/**
 * AI助手管理页面 - 轻量级版本
 * 只负责 Tab 切换和组件加载
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

// 组件名称映射表（菜单名称 → 组件文件名）
const COMPONENT_NAME_MAP = {
    '会话历史': 'session-history',
    '知识库': 'knowledge-management',
    '模型管理': 'model-management',
    '用户管理': 'user-management',
    '角色管理': 'role-management'
};

// Tab ID 映射表（菜单名称 → Tab 内容容器 ID）
const TAB_ID_MAP = {
    '会话历史': 'sessions-tab-content',
    '知识库': 'knowledge-tab-content',
    '模型管理': 'models-tab-content',
    '用户管理': 'user-tab-content',
    '角色管理': 'role-tab-content'
};

/**
 * 获取 Tab 内容容器 ID
 * @param {Object} child - 菜单子项
 * @returns {string} Tab 内容容器的 ID
 */
function getContentId(child) {
    if (TAB_ID_MAP[child.name]) {
        return TAB_ID_MAP[child.name];
    }
    return child.name.toLowerCase().replace(/[\s\u4e00-\u9fa5]+/g, '-') + '-tab-content';
}

/**
 * 加载并初始化组件
 * @param {Object} child - 菜单子项
 */
async function loadComponentForTab(child) {
    const componentName = COMPONENT_NAME_MAP[child.name];
    if (!componentName) {
        console.warn('[AiManage] 未找到菜单 "' + child.name + '" 对应的组件映射');
        return;
    }

    console.log('[AiManage] 加载组件: ' + componentName);

    // 1. 先加载模板文件
    const templateScriptId = 'component-template-' + componentName;
    if (!document.getElementById(templateScriptId)) {
        try {
            await new Promise(function(resolve) {
                var templateScript = document.createElement('script');
                templateScript.id = templateScriptId;
                templateScript.src = 'components/' + componentName + '.template.js?v=' + Date.now();
                templateScript.onload = function() {
                    console.log('[AiManage] 模板加载成功: ' + componentName + '.template.js');
                    resolve();
                };
                templateScript.onerror = function() {
                    console.warn('[AiManage] 模板加载失败: ' + componentName + '.template.js');
                    resolve();
                };
                document.head.appendChild(templateScript);
            });
        } catch (err) {
            console.error('[AiManage] 加载模板异常:', err);
        }
    }

    // 2. 再加载组件逻辑文件，传递 containerId 选项
    const containerId = getContentId(child);
    await loadAndInitComponents([{ name: componentName, options: { containerId: containerId } }]);
}

document.addEventListener('DOMContentLoaded', function() {
    // 动态初始化 Tab 导航
    initPageTabs({
        pageUrls: ['ai-manage.html'],
        tabContainerSelector: '.manage-tabs',
        tabContentSelector: '.manage-tab-content',
        getContentId: getContentId,
        onTabSwitch: async function(child) {
            await loadComponentForTab(child);
        }
    });
});
