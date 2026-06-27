/**
 * 前端认证逻辑
 * 支持用户名/密码登录、注册、微信/钉钉/支付宝扫码登录
 */

// ===== 弹窗控制 =====

function showAuthModal(tab) {
    const overlay = document.getElementById('authOverlay');
    if (!overlay) return;
    overlay.style.display = 'flex';
    switchAuthTab(tab || 'login');
}

function closeAuthModal() {
    const overlay = document.getElementById('authOverlay');
    if (overlay) overlay.style.display = 'none';
    // 清除错误信息
    hideError('loginError');
    hideError('regError');
}

function switchAuthTab(tab) {
    const tabLogin = document.getElementById('tabLogin');
    const tabRegister = document.getElementById('tabRegister');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    if (!tabLogin) return;

    if (tab === 'login') {
        tabLogin.classList.add('active');
        tabRegister.classList.remove('active');
        loginForm.style.display = 'flex';
        registerForm.style.display = 'none';
    } else {
        tabRegister.classList.add('active');
        tabLogin.classList.remove('active');
        registerForm.style.display = 'flex';
        loginForm.style.display = 'none';
    }
    hideError('loginError');
    hideError('regError');
}

// 点击遮罩层关闭弹窗
document.addEventListener('DOMContentLoaded', () => {
    const overlay = document.getElementById('authOverlay');
    if (overlay) {
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) closeAuthModal();
        });
    }
});

// ESC 关闭弹窗
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeAuthModal();
});

// ===== 错误显示 =====

function showError(elementId, msg) {
    const el = document.getElementById(elementId);
    if (el) {
        el.textContent = msg;
        el.style.display = 'block';
    }
}

function hideError(elementId) {
    const el = document.getElementById(elementId);
    if (el) {
        el.style.display = 'none';
        el.textContent = '';
    }
}

// ===== 登录 =====

async function doLogin() {
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;

    if (!username) { showError('loginError', '请输入用户名'); return; }
    if (!password) { showError('loginError', '请输入密码'); return; }

    const btn = document.querySelector('#loginForm .auth-submit');
    btn.disabled = true;
    btn.textContent = '登录中...';
    hideError('loginError');

    try {
        const resp = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await resp.json();

        if (data.success) {
            closeAuthModal();
            // 刷新导航栏认证状态
            if (typeof refreshAuthUI === 'function') refreshAuthUI();
            // 清空表单
            document.getElementById('loginUsername').value = '';
            document.getElementById('loginPassword').value = '';
        } else {
            showError('loginError', data.error || data.message || '登录失败');
        }
    } catch (err) {
        showError('loginError', '网络异常，请稍后重试');
    } finally {
        btn.disabled = false;
        btn.textContent = '登 录';
    }
}

// ===== 注册 =====

async function doRegister() {
    const username = document.getElementById('regUsername').value.trim();
    const nickname = document.getElementById('regNickname').value.trim();
    const password = document.getElementById('regPassword').value;
    const password2 = document.getElementById('regPassword2').value;

    if (!username || username.length < 3) { showError('regError', '用户名至少3位'); return; }
    if (!password || password.length < 6) { showError('regError', '密码至少6位'); return; }
    if (password !== password2) { showError('regError', '两次密码不一致'); return; }

    const btn = document.querySelector('#registerForm .auth-submit');
    btn.disabled = true;
    btn.textContent = '注册中...';
    hideError('regError');

    try {
        const resp = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, nickname: nickname || undefined })
        });
        const data = await resp.json();

        if (data.success) {
            // 注册成功，自动切换到登录
            switchAuthTab('login');
            document.getElementById('loginUsername').value = username;
            document.getElementById('loginPassword').value = '';
            document.getElementById('loginPassword').focus();
            // 清空注册表单
            document.getElementById('regUsername').value = '';
            document.getElementById('regNickname').value = '';
            document.getElementById('regPassword').value = '';
            document.getElementById('regPassword2').value = '';
        } else {
            showError('regError', data.error || data.message || '注册失败');
        }
    } catch (err) {
        showError('regError', '网络异常，请稍后重试');
    } finally {
        btn.disabled = false;
        btn.textContent = '注 册';
    }
}

// ===== 微信扫码登录 =====

async function startWechatLogin() {
    hideError('loginError');
    try {
        const resp = await fetch('/auth/wechat/login');
        const data = await resp.json();

        if (data.success && data.authorizeUrl) {
            window.location.href = data.authorizeUrl;
        } else {
            showError('loginError', data.message || '获取授权链接失败');
        }
    } catch (err) {
        showError('loginError', '网络异常，请稍后重试');
    }
}

// ===== 钉钉扫码登录 =====

async function startDingtalkLogin() {
    hideError('loginError');
    try {
        const resp = await fetch('/auth/dingtalk/login');
        const data = await resp.json();

        if (data.success && data.authorizeUrl) {
            window.location.href = data.authorizeUrl;
        } else {
            showError('loginError', data.message || '获取钉钉授权链接失败');
        }
    } catch (err) {
        showError('loginError', '网络异常，请稍后重试');
    }
}

// ===== 支付宝扫码登录 =====

async function startAlipayLogin() {
    hideError('loginError');
    try {
        const resp = await fetch('/auth/alipay/login');
        const data = await resp.json();

        if (data.success && data.authorizeUrl) {
            window.location.href = data.authorizeUrl;
        } else {
            showError('loginError', data.message || '获取支付宝授权链接失败');
        }
    } catch (err) {
        showError('loginError', '网络异常，请稍后重试');
    }
}

// ===== 回车键提交 =====
document.addEventListener('DOMContentLoaded', () => {
    // 登录表单回车
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') doLogin();
        });
    }
    // 注册表单回车
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') doRegister();
        });
    }
});
