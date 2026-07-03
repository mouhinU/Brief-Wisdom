/**
 * 公共导航栏组件
 * 从 /api/menu/list 接口动态加载菜单并渲染
 * 右侧显示登录/注册按钮或用户信息
 */

// 全局登录状态
let isLoggedIn = false;
// 当前用户角色列表（用于菜单权限校验）
let currentUserRoles = [];
// 当前用户权限标识列表（用于细粒度权限校验）
let currentUserPermissions = [];
// 是否为超级管理员（super_admin 拥有所有权限）
let isSuperAdmin = false;

// ===== 全局 fetch 拦截器：处理 401 未登录 / 403 权限不足 =====
const _originalFetch = window.fetch;
window.fetch = async function(...args) {
  const response = await _originalFetch.apply(this, args);
  const url = (typeof args[0] === 'string' ? args[0] : args[0]?.url) || '';

  // 跳过认证接口本身（登录/注册/状态检查等接口不需要拦截）
  const isAuthApi = url.startsWith('/auth/') || url.startsWith('/api/auth/');

  if (!isAuthApi && response.status === 401) {
    showGlobalToast('登录已过期，请重新登录', 'login');
  } else if (!isAuthApi && response.status === 403) {
    showGlobalToast('权限不足，无法访问该功能', 'error');
  }

  return response;
};

async function initNavbar() {
  try {
    // 0. 初始化国际化框架
    if (window.I18n) {
      await I18n.init();
      // 同步语言选择器
      const langSelect = document.getElementById('navbarLangSelect');
      if (langSelect) langSelect.value = I18n.getLocale();
    }

    // 1. 先检查登录状态，确定用户角色
    await checkLoginStatus();
    // 2. 再基于登录状态加载菜单（后端会根据 session 中的用户角色过滤）
    const res = await fetch('/api/menu/tree');
    const result = await res.json();
    if (!result.success) {
      console.error('加载菜单失败:', result.error);
      return;
    }
    const menus = result.data;
    renderNavbar(menus);

    // 3. 渲染后同步语言选择器（renderNavbar 会重建 DOM）
    const langSelect = document.getElementById('navbarLangSelect');
    if (langSelect && window.I18n) {
      langSelect.value = I18n.getLocale();
    }
  } catch (err) {
    console.error('加载菜单异常:', err);
  }
}

function renderNavbar(menus) {
  // 创建导航栏元素
  const navbar = document.createElement('nav');
  navbar.className = 'navbar';

  // 品牌名
  const brand = document.createElement('a');
  brand.href = '/';
  brand.className = 'navbar-brand';
  brand.textContent = '简知';
  navbar.appendChild(brand);

  // 菜单列表
  const menuList = document.createElement('ul');
  menuList.className = 'navbar-menu';

  const currentPath = window.location.pathname;

  menus.forEach(menu => {
    const li = document.createElement('li');
    
    // 目录类型菜单（type=0）需要展示下拉
    if (menu.type === 0 && menu.children && menu.children.length > 0) {
      li.className = 'menu-dropdown';
      const a = document.createElement('a');
      a.href = 'javascript:void(0)';
      a.className = 'dropdown-trigger';
      a.onclick = function(e) { 
        e.preventDefault(); 
        e.stopPropagation();
        // 关闭其他下拉
        document.querySelectorAll('.menu-dropdown.open').forEach(el => {
          if (el !== li) el.classList.remove('open');
        });
        li.classList.toggle('open'); 
      };
      
      // 图标
      if (menu.icon) {
        const iconSpan = document.createElement('span');
        iconSpan.className = 'menu-icon';
        iconSpan.textContent = menu.icon;
        a.appendChild(iconSpan);
      }
      
      // 文字
      const textSpan = document.createElement('span');
      textSpan.className = 'menu-text';
      textSpan.textContent = menu.name;
      a.appendChild(textSpan);
      
      // 下拉箭头
      const arrowSpan = document.createElement('span');
      arrowSpan.className = 'dropdown-arrow';
      arrowSpan.textContent = '▼';
      a.appendChild(arrowSpan);
      
      li.appendChild(a);
      
      // 子菜单
      const ul = document.createElement('ul');
      ul.className = 'dropdown-menu';
      menu.children.forEach(child => {
        const childLi = document.createElement('li');
        const childA = document.createElement('a');
        childA.href = child.url || '#';
        if (child.target && child.target !== '_self') {
          childA.target = child.target;
        }
        
        // 拦截菜单点击：登录校验 + 权限校验
        childA.onclick = function(e) {
          if (!checkMenuPermission(child)) {
            e.preventDefault();
          }
        };
        
        // 判断当前页面是否匹配
        const childUrl = child.url || '';
        const childPath = childUrl.split('#')[0];
        if (currentPath === childPath) {
          childA.classList.add('active');
          li.classList.add('has-active-child');
        }
        
        // 图标
        if (child.icon) {
          const childIconSpan = document.createElement('span');
          childIconSpan.className = 'menu-icon';
          childIconSpan.textContent = child.icon;
          childA.appendChild(childIconSpan);
        }
        
        // 文字
        const childTextSpan = document.createElement('span');
        childTextSpan.className = 'menu-text';
        childTextSpan.textContent = child.name;
        childA.appendChild(childTextSpan);
        
        childLi.appendChild(childA);
        ul.appendChild(childLi);
      });
      li.appendChild(ul);
    } else {
      // 普通菜单
      const a = document.createElement('a');
      a.href = menu.url || '#';
      if (menu.target && menu.target !== '_self') {
        a.target = menu.target;
      }

      // 拦截菜单点击：登录校验 + 权限校验
      a.onclick = function(e) {
        if (!checkMenuPermission(menu)) {
          e.preventDefault();
        }
      };

      // 判断当前页面是否匹配（url 可能为 null）
      const menuUrl = menu.url || '';
      const menuPath = menuUrl.split('#')[0]; // 去掉 hash
      if (menuPath === '/' && (currentPath === '/' || currentPath === '/index.html')) {
        a.classList.add('active');
      } else if (menuPath !== '/' && currentPath === menuPath) {
        a.classList.add('active');
      }

      // 图标
      if (menu.icon) {
        const iconSpan = document.createElement('span');
        iconSpan.className = 'menu-icon';
        iconSpan.textContent = menu.icon;
        a.appendChild(iconSpan);
      }

      // 文字
      const textSpan = document.createElement('span');
      textSpan.className = 'menu-text';
      textSpan.textContent = menu.name;
      a.appendChild(textSpan);

      li.appendChild(a);
    }
    
    menuList.appendChild(li);
  });

  navbar.appendChild(menuList);

  // ===== 语言切换 + 右侧认证区域 =====
  const rightArea = document.createElement('div');
  rightArea.className = 'navbar-right';

  // 语言切换器
  const langSwitcher = document.createElement('div');
  langSwitcher.className = 'navbar-lang';
  langSwitcher.innerHTML = `
    <select id="navbarLangSelect" onchange="I18n && I18n.setLocale(this.value)">
      <option value="zh-CN">中文</option>
      <option value="en-US">EN</option>
    </select>
  `;
  rightArea.appendChild(langSwitcher);

  // 认证区域
  const authArea = document.createElement('div');
  authArea.className = 'navbar-auth';
  authArea.id = 'navbarAuth';
  // 先渲染未登录状态
  authArea.innerHTML = `
    <button class="auth-btn auth-login-btn" onclick="window.location.href='/login.html?tab=login'">登录</button>
    <button class="auth-btn auth-register-btn" onclick="window.location.href='/login.html?tab=register'">注册</button>
  `;
  rightArea.appendChild(authArea);
  navbar.appendChild(rightArea);

  // 插入到 body 最前面
  document.body.insertBefore(navbar, document.body.firstChild);

  // 动态注入 AI 智能助手组件（所有页面都可用）
  injectAiAssistant();

  // 动态加载 AI 助手脚本（如果未加载）
  loadChatScriptsIfNeeded();

  // 初始化后不再重复检查（已在 initNavbar 开头检查过）

  // 基于已检查的登录状态更新认证区域 UI
  updateAuthArea();
}



/**
 * 动态注入 AI 智能助手悬浮按钮和聊天窗口 HTML
 * 如果页面中已存在 aiFab 则跳过
 */
function injectAiAssistant() {
  if (document.getElementById('aiFab')) return;

  // 优先使用模板生成器（组件化架构）
  if (window.AiAssistantTemplate) {
    console.log('[Navbar] 使用 AiAssistantTemplate 渲染');
    AiAssistantTemplate.render();
  } else {
    // 降级方案：直接创建元素（保持向后兼容）
    console.log('[Navbar] 模板未加载，使用降级方案');
    
    // 悬浮按钮
    const fab = document.createElement('div');
    fab.className = 'ai-fab';
    fab.id = 'aiFab';
    fab.onclick = function() { toggleChat(); };
    fab.innerHTML = '<span class="ai-fab-icon">🤖</span>';
    document.body.appendChild(fab);

    // 聊天弹窗
    const popup = document.createElement('div');
    popup.className = 'chat-popup';
    popup.id = 'chatPopup';
    popup.innerHTML = `
      <div class="session-sidebar">
        <div class="session-header">
          <h2>💬 会话历史</h2>
        </div>
        <div class="session-list" id="sessionList"></div>
        <button class="new-session-btn" onclick="createNewSession()">+ 新建会话</button>
      </div>
      <div class="chat-main">
        <div class="chat-header">
          <h1>🤖 AI 智能助手</h1>
          <p>有任何问题都可以问我哦~</p>
          <button class="close-button" onclick="toggleChat()">×</button>
        </div>
        <div class="chat-messages" id="chatMessages">
          <div class="welcome-message">
            <p>请输入您的问题,我会尽力为您解答</p>
          </div>
        </div>
        <div class="typing-indicator" id="typingIndicator">
          <span></span><span></span><span></span>
        </div>
        <div class="chat-input-container">
          <div class="chat-input-wrapper">
            <select id="modelSelector" class="model-selector-select" onchange="onModelChange()">
              <option value="">加载中...</option>
            </select>
            <input type="text" class="chat-input" id="chatInput"
                   placeholder="输入您的问题..." autocomplete="off">
            <button class="send-button" id="sendButton" onclick="sendMessage()">发送</button>
          </div>
        </div>
      </div>
    `;
    document.body.appendChild(popup);
  }
}

/**
 * 动态加载 marked.js 和 chat.js（如果页面未引入）
 */
function loadChatScriptsIfNeeded() {
  // 优先加载 AI 助手模板（组件化架构）
  if (!window.AiAssistantTemplate) {
    const templateScript = document.createElement('script');
    templateScript.src = 'components/ai-assistant.template.js?v=1';
    document.head.appendChild(templateScript);
  }

  // 加载 marked.js（chat.js 依赖它）
  if (typeof marked === 'undefined') {
    const markedScript = document.createElement('script');
    markedScript.src = 'https://cdn.jsdelivr.net/npm/marked/marked.min.js';
    document.head.appendChild(markedScript);
  }
  // 加载 chat.js
  if (typeof toggleChat === 'undefined') {
    const chatScript = document.createElement('script');
    chatScript.src = 'js/chat.js?v=7';
    chatScript.onload = function() {
      // chat.js 加载完成后初始化（会话列表、模型列表等）
      if (typeof initChatApp === 'function') initChatApp();
    };
    document.head.appendChild(chatScript);
  } else {
    // chat.js 已存在但可能是动态注入的，确保初始化已执行
    if (typeof initChatApp === 'function') initChatApp();
  }
}

/**
 * 检查登录状态并刷新导航栏右侧的认证区域
 */
async function refreshAuthUI() {
  // 先检查登录状态（更新全局变量）
  await checkLoginStatus();
  // 更新导航栏右侧认证区域（登录/注册按钮 → 用户头像/昵称）
  updateAuthArea();
  // 重新加载菜单（登录后可能多出管理菜单项）
  try {
    const res = await fetch('/api/menu/tree');
    const result = await res.json();
    if (!result.success) return;
    const menus = result.data;
    // 重新渲染菜单区域
    const oldNavbar = document.querySelector('nav.navbar');
    if (oldNavbar) {
      // 保留导航栏，只更新菜单列表和认证区域
      const oldMenuList = oldNavbar.querySelector('.navbar-menu');
      if (oldMenuList) oldMenuList.remove();
      // 构建新菜单列表
      const newMenuList = buildMenuList(menus);
      const brand = oldNavbar.querySelector('.navbar-brand');
      if (brand && brand.nextSibling) {
        oldNavbar.insertBefore(newMenuList, brand.nextSibling);
      } else {
        oldNavbar.appendChild(newMenuList);
      }
    }
  } catch (err) {
    console.error('刷新菜单失败:', err);
  }
}

/**
 * 构建菜单列表 DOM（从 renderNavbar 中提取，复用菜单渲染逻辑）
 */
function buildMenuList(menus) {
  const menuList = document.createElement('ul');
  menuList.className = 'navbar-menu';
  const currentPath = window.location.pathname;

  menus.forEach(menu => {
    const li = document.createElement('li');
    if (menu.type === 0 && menu.children && menu.children.length > 0) {
      li.className = 'menu-dropdown';
      const a = document.createElement('a');
      a.href = 'javascript:void(0)';
      a.className = 'dropdown-trigger';
      a.onclick = function(e) {
        e.preventDefault(); e.stopPropagation();
        document.querySelectorAll('.menu-dropdown.open').forEach(el => { if (el !== li) el.classList.remove('open'); });
        li.classList.toggle('open');
      };
      if (menu.icon) { const s = document.createElement('span'); s.className = 'menu-icon'; s.textContent = menu.icon; a.appendChild(s); }
      const t = document.createElement('span'); t.className = 'menu-text'; t.textContent = menu.name; a.appendChild(t);
      const arrow = document.createElement('span'); arrow.className = 'dropdown-arrow'; arrow.textContent = '▼'; a.appendChild(arrow);
      li.appendChild(a);
      const ul = document.createElement('ul'); ul.className = 'dropdown-menu';
      menu.children.forEach(child => {
        const childLi = document.createElement('li');
        const childA = document.createElement('a');
        childA.href = child.url || '#';
        if (child.target && child.target !== '_self') childA.target = child.target;
        childA.onclick = function(e) { if (!checkMenuPermission(child)) e.preventDefault(); };
        const childPath = (child.url || '').split('#')[0];
        if (currentPath === childPath) { childA.classList.add('active'); li.classList.add('has-active-child'); }
        if (child.icon) { const s = document.createElement('span'); s.className = 'menu-icon'; s.textContent = child.icon; childA.appendChild(s); }
        const ct = document.createElement('span'); ct.className = 'menu-text'; ct.textContent = child.name; childA.appendChild(ct);
        childLi.appendChild(childA); ul.appendChild(childLi);
      });
      li.appendChild(ul);
    } else {
      const a = document.createElement('a');
      a.href = menu.url || '#';
      if (menu.target && menu.target !== '_self') a.target = menu.target;
      a.onclick = function(e) { if (!checkMenuPermission(menu)) e.preventDefault(); };
      const menuPath = (menu.url || '').split('#')[0];
      if (menuPath === '/' && (currentPath === '/' || currentPath === '/index.html')) a.classList.add('active');
      else if (menuPath !== '/' && currentPath === menuPath) a.classList.add('active');
      if (menu.icon) { const s = document.createElement('span'); s.className = 'menu-icon'; s.textContent = menu.icon; a.appendChild(s); }
      const t = document.createElement('span'); t.className = 'menu-text'; t.textContent = menu.name; a.appendChild(t);
      li.appendChild(a);
    }
    menuList.appendChild(li);
  });
  return menuList;
}

/**
 * 检查登录状态，更新全局变量（不依赖 DOM）
 */
async function checkLoginStatus() {
  try {
    const resp = await fetch('/api/auth/status');
    const data = await resp.json();
    if (data.loggedIn && data.user) {
      isLoggedIn = true;
      currentUserRoles = data.roles || [];
      currentUserPermissions = data.permissions || [];
      isSuperAdmin = data.isSuperAdmin === true;
      window._currentUser = data.user;
    } else {
      isLoggedIn = false;
      currentUserRoles = [];
      currentUserPermissions = [];
      isSuperAdmin = false;
      window._currentUser = null;
    }
  } catch (err) {
    console.error('检查登录状态失败:', err);
    isLoggedIn = false;
    currentUserRoles = [];
    currentUserPermissions = [];
    isSuperAdmin = false;
  }
}

/**
 * 更新导航栏右侧的认证区域 UI
 */
function updateAuthArea() {
  const authArea = document.getElementById('navbarAuth');
  if (!authArea) return;
  if (isLoggedIn && window._currentUser) {
    const nickname = window._currentUser.nickname || window._currentUser.username || '用户';
    const avatar = window._currentUser.avatar || '';
    authArea.innerHTML = `
      <div class="auth-user-area">
        <div class="auth-avatar" onclick="toggleUserMenu()">
          ${avatar ? '<img src="' + avatar + '" alt="avatar">' : nickname.charAt(0)}
        </div>
        <span class="auth-nickname" onclick="toggleUserMenu()">${nickname}</span>
        <div class="auth-user-dropdown" id="userDropdown">
          <a href="/about.html" class="dropdown-item">👤 我的简历</a>
          <button class="dropdown-item dropdown-logout" onclick="doLogout()">🚪 退出登录</button>
        </div>
      </div>
    `;
  } else {
    authArea.innerHTML = `
      <button class="auth-btn auth-login-btn" onclick="window.location.href='/login.html?tab=login'">登录</button>
      <button class="auth-btn auth-register-btn" onclick="window.location.href='/login.html?tab=register'">注册</button>
    `;
  }
}

/**
 * 切换用户下拉菜单
 */
function toggleUserMenu() {
  const dropdown = document.getElementById('userDropdown');
  if (dropdown) {
    dropdown.classList.toggle('show');
  }
}

// 点击其他地方关闭用户菜单
document.addEventListener('click', (e) => {
  const dropdown = document.getElementById('userDropdown');
  if (dropdown && !e.target.closest('.auth-user-area')) {
    dropdown.classList.remove('show');
  }
});

/**
 * 退出登录
 */
async function doLogout() {
  try {
    await fetch('/auth/logout', { method: 'POST' });
  } catch (e) {
    console.error('退出失败:', e);
  }
  // 直接跳转首页（避免 reload 当前页面因需要登录而显示异常）
  window.location.href = '/';
}

/**
 * 菜单权限校验
 * @param {Object} menu 菜单对象（包含 requireLogin, permission 等属性）
 * @returns {boolean} 是否允许访问
 */
function checkMenuPermission(menu) {
  // 1. 登录校验
  if (menu.requireLogin === 1 && !isLoggedIn) {
    showGlobalToast('当前未登录，请先登录后再访问', 'login');
    return false;
  }
  // 2. 权限校验：如果菜单设置了 permission，检查用户是否拥有对应权限
  if (menu.permission) {
    // 超级管理员拥有所有权限
    if (isSuperAdmin) return true;
    // 检查用户权限列表是否包含该权限标识
    if (!currentUserPermissions.includes(menu.permission)) {
      showGlobalToast('权限不足，无法访问该功能', 'error');
      return false;
    }
  }
  return true;
}

/**
 * 检查当前用户是否拥有指定权限
 * @param {string} permission 权限标识
 * @returns {boolean} 是否拥有权限
 */
function hasPermission(permission) {
  if (!isLoggedIn) return false;
  if (isSuperAdmin) return true;
  return currentUserPermissions.includes(permission);
}

/**
 * 显示全局 Toast 提示（支持未登录和权限不足两种场景）
 * @param {string} message 提示消息
 * @param {string} type 'login' 显示去登录按钮 | 'error' 仅提示
 */
function showGlobalToast(message, type) {
  // 移除已存在的 toast
  const existing = document.getElementById('globalToast');
  if (existing) existing.remove();

  const toast = document.createElement('div');
  toast.id = 'globalToast';
  toast.className = 'login-required-toast';

  const icon = type === 'login' ? '⚠️' : '🔒';
  let actionBtn = '';
  if (type === 'login') {
    actionBtn = `<button class="toast-login-btn" onclick="closeGlobalToast();window.location.href='/login.html?tab=login'">去登录</button>`;
  }

  toast.innerHTML = `
    <span class="toast-icon">${icon}</span>
    <span class="toast-text">${message}</span>
    ${actionBtn}
  `;
  document.body.appendChild(toast);

  // 3秒后自动跳转首页
  setTimeout(() => {
    closeGlobalToast();
    // 如果不在首页才跳转
    if (window.location.pathname !== '/') {
      window.location.href = '/';
    }
  }, 3000);
}

function closeGlobalToast() {
  const toast = document.getElementById('globalToast');
  if (toast) toast.remove();
}

/**
 * 显示操作反馈 Toast（居中显示，3秒自动消失）
 * @param {string} message 提示消息
 * @param {string} type 'success' | 'error'，默认 'success'
 */
function showToast(message, type) {
  type = type || 'success';
  // 移除已存在的 toast
  const existing = document.getElementById('opToast');
  if (existing) existing.remove();

  const toast = document.createElement('div');
  toast.id = 'opToast';
  toast.className = 'op-toast op-toast-' + type;

  const icon = type === 'success' ? '✓' : '✕';
  toast.innerHTML = `<span class="op-toast-icon">${icon}</span><span class="op-toast-text">${message}</span>`;
  document.body.appendChild(toast);

  // 3秒后自动消失（带淡出动画）
  setTimeout(() => {
    toast.classList.add('op-toast-fadeout');
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}

/**
 * 显示"当前未登录"提示 toast，然后跳转到首页
 * @deprecated 使用 showGlobalToast 替代
 */
function showLoginRequiredToast() {
  showGlobalToast('当前未登录，请先登录后再访问', 'login');
}

/**
 * 全局确认弹窗（居中显示，替代浏览器原生 confirm）
 * @param {string} message 确认消息
 * @param {string} icon 图标（可选，默认 ⚠️）
 * @returns {Promise<boolean>} 用户选择结果
 */
function showConfirmDialog(message, icon) {
  return new Promise((resolve) => {
    // 移除已存在的弹窗
    const existing = document.getElementById('globalConfirmDialog');
    if (existing) existing.remove();

    const overlay = document.createElement('div');
    overlay.id = 'globalConfirmDialog';
    overlay.className = 'global-confirm-overlay';
    overlay.innerHTML = `
      <div class="global-confirm-modal">
        ${icon ? `<div class="global-confirm-icon">${icon}</div>` : ''}
        <div class="global-confirm-message">${message}</div>
        <div class="global-confirm-buttons">
          <button class="global-confirm-cancel">取消</button>
          <button class="global-confirm-ok">确定</button>
        </div>
      </div>
    `;
    document.body.appendChild(overlay);

    const cancelBtn = overlay.querySelector('.global-confirm-cancel');
    const okBtn = overlay.querySelector('.global-confirm-ok');

    const close = (result) => {
      overlay.remove();
      resolve(result);
    };

    cancelBtn.onclick = () => close(false);
    okBtn.onclick = () => close(true);
    // 点击遮罩层取消
    overlay.onclick = (e) => {
      if (e.target === overlay) close(false);
    };
    // ESC 键取消
    const escHandler = (e) => {
      if (e.key === 'Escape') {
        close(false);
        document.removeEventListener('keydown', escHandler);
      }
    };
    document.addEventListener('keydown', escHandler);
    // 默认聚焦确定按钮
    okBtn.focus();
  });
}

/**
 * 动态初始化页面 Tab 导航
 * 从 /api/menu/tree 接口获取当前页面的子菜单，渲染为 Tab 按钮
 * @param {Object} config 页面配置
 * @param {string[]} config.pageUrls - 当前页面的 URL 匹配列表
 * @param {string} config.tabContainerSelector - Tab 导航容器 CSS 选择器
 * @param {string} config.tabContentSelector - Tab 内容区域 CSS 选择器
 * @param {Function} config.getContentId - function(child) 返回对应的内容元素 ID
 * @param {Function} [config.onTabSwitch] - function(child) Tab 切换时的回调
 */
async function initPageTabs(config) {
  try {
    await checkLoginStatus();
    const res = await fetch('/api/menu/tree');
    const result = await res.json();
    if (!result.success) return;

    const menus = result.data || [];
    const currentUrl = window.location.pathname.split('/').pop() || window.location.pathname;

    // 查找当前页面对应的菜单项
    const currentMenu = menus.find(m => {
      if (!m.url) return false;
      const menuUrl = m.url.split('/').pop() || m.url;
      return menuUrl === currentUrl || currentUrl.startsWith(menuUrl.split('?')[0]);
    });

    if (!currentMenu || !currentMenu.children || currentMenu.children.length === 0) {
      return; // 没有子菜单，不渲染 Tab
    }

    const children = currentMenu.children;
    const tabContainer = document.querySelector(config.tabContainerSelector);
    if (!tabContainer) return;

    // 动态渲染 Tab 按钮
    tabContainer.innerHTML = '';
    children.forEach((child, index) => {
      const btn = document.createElement('button');
      btn.className = 'tab-btn';
      if (index === 0) btn.classList.add('active');
      btn.textContent = child.name;
      btn.setAttribute('data-tab-key', child.name);
      btn.onclick = async function() {
        // 切换 Tab 按钮激活状态
        tabContainer.querySelectorAll('button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        // 切换内容区域显示
        const contentId = config.getContentId(child);
        if (contentId) {
          document.querySelectorAll(config.tabContentSelector).forEach(c => c.classList.remove('active'));
          const target = document.getElementById(contentId);
          if (target) target.classList.add('active');
        }
        // 回调
        if (config.onTabSwitch) await config.onTabSwitch(child);
      };
      tabContainer.appendChild(btn);
    });

    // 默认激活第一个 Tab 的内容
    const firstChild = children[0];
    const firstContentId = config.getContentId(firstChild);
    if (firstContentId) {
      document.querySelectorAll(config.tabContentSelector).forEach(c => c.classList.remove('active'));
      const target = document.getElementById(firstContentId);
      if (target) target.classList.add('active');
    }
    if (config.onTabSwitch) await config.onTabSwitch(firstChild);
  } catch (err) {
    console.error('初始化页面Tab失败:', err);
  }
}

// 自动初始化
document.addEventListener('DOMContentLoaded', () => {
  // 嵌入模式（iframe 中加载）不注入导航栏和 AI 助手
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('embedded') === '1') {
    return;
  }
  initNavbar();
});
