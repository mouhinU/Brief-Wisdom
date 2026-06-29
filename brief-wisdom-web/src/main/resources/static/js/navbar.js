/**
 * 公共导航栏组件
 * 从 /api/menu/list 接口动态加载菜单并渲染
 * 右侧显示登录/注册按钮或用户信息
 */

// 全局登录状态
let isLoggedIn = false;

// ===== 全局 fetch 拦截器：处理 401 未登录 / 403 权限不足 =====
const _originalFetch = window.fetch;
window.fetch = async function(...args) {
  const response = await _originalFetch.apply(this, args);
  const url = (args[1] && args[1].url) || (typeof args[0] === 'string' ? args[0] : args[0].url) || '';

  if (response.status === 401) {
    // 未登录：弹窗提示并跳转首页
    showGlobalToast('当前未登录，请先登录后再访问', 'login');
  } else if (response.status === 403) {
    // 权限不足：弹窗提示并跳转首页
    showGlobalToast('权限不足，无法访问该资源', 'error');
  }

  return response;
};

async function initNavbar() {
  try {
    const res = await fetch('/api/menu/tree');
    const result = await res.json();
    if (!result.success) {
      console.error('加载菜单失败:', result.error);
      return;
    }
    const menus = result.data;
    renderNavbar(menus);
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
        
        // 拦截需要登录的菜单点击
        if (child.requireLogin === 1) {
          childA.onclick = function(e) {
            if (!isLoggedIn) {
              e.preventDefault();
              showLoginRequiredToast();
            }
          };
        }
        
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

      // 拦截需要登录的菜单点击
      if (menu.requireLogin === 1) {
        a.onclick = function(e) {
          if (!isLoggedIn) {
            e.preventDefault();
            showLoginRequiredToast();
          }
        };
      }

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

  // ===== 右侧认证区域 =====
  const authArea = document.createElement('div');
  authArea.className = 'navbar-auth';
  authArea.id = 'navbarAuth';
  // 先渲染未登录状态，auth.js 加载后会更新
  authArea.innerHTML = `
    <button class="auth-btn auth-login-btn" onclick="showAuthModal('login')">登录</button>
    <button class="auth-btn auth-register-btn" onclick="showAuthModal('register')">注册</button>
  `;
  navbar.appendChild(authArea);

  // 插入到 body 最前面
  document.body.insertBefore(navbar, document.body.firstChild);

  // 动态注入登录/注册弹窗 HTML（所有页面都可使用）
  injectAuthOverlay();

  // 动态加载 auth.js（如果未加载）
  loadAuthScriptIfNeeded();

  // 动态注入 AI 智能助手组件（所有页面都可用）
  injectAiAssistant();

  // 动态加载 AI 助手脚本（如果未加载）
  loadChatScriptsIfNeeded();

  // 初始化后检查登录状态
  refreshAuthUI();
}

/**
 * 动态注入登录/注册弹窗 HTML
 * 如果页面中已存在 authOverlay 则跳过
 */
function injectAuthOverlay() {
  if (document.getElementById('authOverlay')) return;
  const overlay = document.createElement('div');
  overlay.className = 'auth-overlay';
  overlay.id = 'authOverlay';
  overlay.style.display = 'none';
  overlay.innerHTML = `
    <div class="auth-modal">
      <button class="auth-modal-close" onclick="closeAuthModal()">&times;</button>
      <div class="auth-tabs">
        <button class="auth-tab active" id="tabLogin" onclick="switchAuthTab('login')">登录</button>
        <button class="auth-tab" id="tabRegister" onclick="switchAuthTab('register')">注册</button>
      </div>
      <div class="auth-form-area" id="loginForm">
        <input type="text" class="auth-input" id="loginUsername" placeholder="用户名" autocomplete="username">
        <input type="password" class="auth-input" id="loginPassword" placeholder="密码" autocomplete="current-password">
        <div class="auth-error" id="loginError" style="display:none;"></div>
        <button class="auth-submit" onclick="doLogin()">登 录</button>
        <div class="auth-divider"><span>或通过第三方扫码登录</span></div>
        <div class="auth-oauth-buttons">
          <button class="auth-wechat-btn" onclick="startWechatLogin()"><span>💬</span> 微信</button>
          <button class="auth-dingtalk-btn" onclick="startDingtalkLogin()"><span>📌</span> 钉钉</button>
          <button class="auth-alipay-btn" onclick="startAlipayLogin()"><span>💰</span> 支付宝</button>
        </div>
      </div>
      <div class="auth-form-area" id="registerForm" style="display:none;">
        <input type="text" class="auth-input" id="regUsername" placeholder="用户名（至少3位）" autocomplete="username">
        <input type="text" class="auth-input" id="regNickname" placeholder="昵称（可选）">
        <input type="password" class="auth-input" id="regPassword" placeholder="密码（至少6位）" autocomplete="new-password">
        <input type="password" class="auth-input" id="regPassword2" placeholder="确认密码" autocomplete="new-password">
        <div class="auth-error" id="regError" style="display:none;"></div>
        <button class="auth-submit" onclick="doRegister()">注 册</button>
      </div>
    </div>
  `;
  document.body.appendChild(overlay);
}

/**
 * 动态加载 auth.js（如果页面未引入）
 */
function loadAuthScriptIfNeeded() {
  // 检查 auth.js 是否已加载
  if (typeof showAuthModal === 'function') return;
  const script = document.createElement('script');
  script.src = 'js/auth.js?v=5';
  document.head.appendChild(script);
}

/**
 * 动态注入 AI 智能助手悬浮按钮和聊天窗口 HTML
 * 如果页面中已存在 aiFab 则跳过
 */
function injectAiAssistant() {
  if (document.getElementById('aiFab')) return;

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

/**
 * 动态加载 marked.js 和 chat.js（如果页面未引入）
 */
function loadChatScriptsIfNeeded() {
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
  const authArea = document.getElementById('navbarAuth');
  if (!authArea) return;
  try {
    const resp = await fetch('/api/auth/status');
    const data = await resp.json();
    if (data.loggedIn && data.user) {
      isLoggedIn = true;
      const nickname = data.user.nickname || data.user.username || '用户';
      const avatar = data.user.avatar || '';
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
      isLoggedIn = false;
      authArea.innerHTML = `
        <button class="auth-btn auth-login-btn" onclick="showAuthModal('login')">登录</button>
        <button class="auth-btn auth-register-btn" onclick="showAuthModal('register')">注册</button>
      `;
    }
  } catch (err) {
    console.error('检查登录状态失败:', err);
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
  // 刷新页面状态
  window.location.reload();
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
    actionBtn = `<button class="toast-login-btn" onclick="closeGlobalToast();showAuthModal('login')">去登录</button>`;
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
 * 显示“当前未登录”提示 toast，然后跳转到首页
 * @deprecated 使用 showGlobalToast 替代
 */
function showLoginRequiredToast() {
  showGlobalToast('当前未登录，请先登录后再访问', 'login');
}

// 自动初始化
document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
});
