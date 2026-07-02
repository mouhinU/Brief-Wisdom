/**
 * 组件加载器
 * 用于动态加载和管理页面组件
 */

// 组件注册表
const componentRegistry = {};

/**
 * 注册组件
 * @param {string} name - 组件名称
 * @param {Object} component - 组件对象（包含 init, destroy 等方法）
 */
function registerComponent(name, component) {
    if (componentRegistry[name]) {
        console.warn(`组件 ${name} 已被注册，将被覆盖`);
    }
    componentRegistry[name] = component;
    console.log(`[Component] 组件 "${name}" 已注册`);
}

/**
 * 获取组件
 * @param {string} name - 组件名称
 * @returns {Object|null} 组件对象
 */
function getComponent(name) {
    return componentRegistry[name] || null;
}

/**
 * 初始化组件
 * @param {string} name - 组件名称
 * @param {Object} options - 初始化选项
 */
async function initComponent(name, options = {}) {
    const component = getComponent(name);
    if (!component) {
        console.error(`[Component] 组件 "${name}" 未找到`);
        return false;
    }

    try {
        if (typeof component.init === 'function') {
            await component.init(options);
            console.log(`[Component] 组件 "${name}" 初始化成功`);
            return true;
        }
    } catch (err) {
        console.error(`[Component] 组件 "${name}" 初始化失败:`, err);
        return false;
    }
}

/**
 * 销毁组件
 * @param {string} name - 组件名称
 */
function destroyComponent(name) {
    const component = getComponent(name);
    if (!component) return;

    try {
        if (typeof component.destroy === 'function') {
            component.destroy();
            console.log(`[Component] 组件 "${name}" 已销毁`);
        }
    } catch (err) {
        console.error(`[Component] 组件 "${name}" 销毁失败:`, err);
    }
}

/**
 * 动态加载组件脚本
 * @param {string} componentName - 组件名称（对应文件名）
 * @returns {Promise<void>}
 */
function loadComponentScript(componentName) {
    return new Promise((resolve, reject) => {
        // 检查是否已加载
        const scriptId = `component-${componentName}`;
        if (document.getElementById(scriptId)) {
            console.log(`[Component] 脚本 "${componentName}" 已存在`);
            resolve();
            return;
        }

        const script = document.createElement('script');
        script.id = scriptId;
        script.src = `components/${componentName}.js?v=${Date.now()}`;
        script.onload = () => {
            console.log(`[Component] 脚本 "${componentName}" 加载成功`);
            resolve();
        };
        script.onerror = () => {
            console.error(`[Component] 脚本 "${componentName}" 加载失败`);
            reject(new Error(`Failed to load component: ${componentName}`));
        };
        document.head.appendChild(script);
    });
}

/**
 * 批量加载并初始化组件
 * @param {Array<{name: string, options?: Object}>} components - 组件列表
 */
async function loadAndInitComponents(components) {
    for (const comp of components) {
        try {
            await loadComponentScript(comp.name);
            await initComponent(comp.name, comp.options || {});
        } catch (err) {
            console.error(`[Component] 加载组件 "${comp.name}" 失败:`, err);
        }
    }
}
