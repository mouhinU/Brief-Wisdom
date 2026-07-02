/**
 * 系统设置页面 - 轻量级版本
 * 只负责 Tab 切换和组件加载
 * 
 * 注意：此脚本可以在任何包含 .settings-tabs 和 .settings-tab-content 的页面中使用
 * 只需在数据库中配置相应的菜单结构即可
 */

// 组件名称映射表（菜单名称 → 组件文件名）
const COMPONENT_NAME_MAP = {
    '菜单管理': 'menu-management',
    '用户管理': 'user-management',
    '角色管理': 'role-management',
    '知识库': 'knowledge-management',
    '模型管理': 'model-management',
    '会话历史': 'session-history'
};

// Tab ID 映射表（菜单名称 → Tab 内容容器 ID）
const TAB_ID_MAP = {
    '菜单管理': 'menu-tab-content',
    '用户管理': 'user-tab-content',
    '角色管理': 'role-tab-content',
    '知识库': 'knowledge-tab-content',
    '模型管理': 'models-tab-content',
    '会话历史': 'sessions-tab-content'
};

/**
 * 获取 Tab 内容容器 ID
 * @param {Object} child - 菜单子项
 * @returns {string} Tab 内容容器的 ID
 */
function getContentId(child) {
    // 优先使用映射表
    if (TAB_ID_MAP[child.name]) {
        return TAB_ID_MAP[child.name];
    }
    // 否则尝试自动生成（将中文名称转换为 kebab-case）
    return child.name.toLowerCase().replace(/[\s\u4e00-\u9fa5]+/g, '-') + '-tab-content';
}

/**
 * 加载并初始化组件
 * @param {Object} child - 菜单子项
 */
async function loadComponentForTab(child) {
    const componentName = COMPONENT_NAME_MAP[child.name];
    if (!componentName) {
        console.warn(`[SystemSettings] 未找到菜单 "${child.name}" 对应的组件映射`);
        return;
    }
    
    console.log(`[SystemSettings] 加载组件: ${componentName}`);
    await loadAndInitComponents([{ name: componentName }]);
}

document.addEventListener('DOMContentLoaded', () => {
    // 动态初始化 Tab 导航（数据来源于菜单接口的 children）
    initPageTabs({
        pageUrls: ['system-settings.html'],
        tabContainerSelector: '.settings-tabs',
        tabContentSelector: '.settings-tab-content',
        getContentId: getContentId,
        onTabSwitch: async function(child) {
            await loadComponentForTab(child);
        }
    });
});
