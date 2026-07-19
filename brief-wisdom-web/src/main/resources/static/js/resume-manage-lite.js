/**
 * 简历管理页面 - 轻量级版本
 * 只负责 Tab 切换和组件加载
 *
 * @author Brief-Wisdom
 * @date 2026-07-17
 */

// 组件名称映射表（菜单名称 → 组件文件名）
const RESUME_COMPONENT_MAP = {
    '在线编辑': 'online-editor',
    '工作经历': 'experience-management',
    '项目经历': 'project-management',
    '项目成果': 'achievement-management',
    '技术栈': 'tech-stack-management'
};

// Tab ID 映射表（菜单名称 → Tab 内容容器 ID）
const RESUME_TAB_ID_MAP = {
    '在线编辑': 'editor-tab-content',
    '工作经历': 'experience-tab-content',
    '项目经历': 'project-tab-content',
    '项目成果': 'achievement-tab-content',
    '技术栈': 'tech-stack-tab-content'
};

/**
 * 获取 Tab 内容容器 ID
 * @param {Object} child - 菜单子项
 * @returns {string} Tab 内容容器的 ID
 */
function getResumeContentId(child) {
    if (RESUME_TAB_ID_MAP[child.name]) {
        return RESUME_TAB_ID_MAP[child.name];
    }
    return child.name.toLowerCase().replace(/[\s\u4e00-\u9fa5]+/g, '-') + '-tab-content';
}

/**
 * 加载并初始化组件
 * @param {Object} child - 菜单子项
 */
async function loadResumeComponent(child) {
    const componentName = RESUME_COMPONENT_MAP[child.name];
    if (!componentName) {
        console.warn('[ResumeManage] 未找到菜单 "' + child.name + '" 对应的组件映射');
        return;
    }

    console.log('[ResumeManage] 加载组件: ' + componentName);

    // 1. 先加载模板文件
    const templateScriptId = 'component-template-' + componentName;
    if (!document.getElementById(templateScriptId)) {
        try {
            await new Promise(function(resolve) {
                var templateScript = document.createElement('script');
                templateScript.id = templateScriptId;
                templateScript.src = 'components/' + componentName + '.template.js?v=' + Date.now();
                templateScript.onload = function() {
                    console.log('[ResumeManage] 模板加载成功: ' + componentName + '.template.js');
                    resolve();
                };
                templateScript.onerror = function() {
                    console.warn('[ResumeManage] 模板加载失败: ' + componentName + '.template.js');
                    resolve();
                };
                document.head.appendChild(templateScript);
            });
        } catch (err) {
            console.error('[ResumeManage] 加载模板异常:', err);
        }
    }

    // 2. 加载组件逻辑文件
    var containerId = getResumeContentId(child);
    await loadAndInitComponents([{ name: componentName, options: { containerId: containerId } }]);
}

document.addEventListener('DOMContentLoaded', function() {
    initPageTabs({
        pageUrls: ['resume-manage.html'],
        tabContainerSelector: '.tabs',
        tabContentSelector: '.tab-content',
        getContentId: getResumeContentId,
        onTabSwitch: async function(child) {
            await loadResumeComponent(child);
        }
    });
});
