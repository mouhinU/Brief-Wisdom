/**
 * 登录/注册独立页面逻辑
 * 用于 login.html 页面，提供登录、手机号登录、注册和第三方扫码功能
 */

// ===== Tab 切换 =====

function switchAuthTab(tab) {
    const tabLogin = document.getElementById('tabLogin');
    const tabPhone = document.getElementById('tabPhone');
    const tabRegister = document.getElementById('tabRegister');
    const loginForm = document.getElementById('loginForm');
    const phoneLoginForm = document.getElementById('phoneLoginForm');
    const registerForm = document.getElementById('registerForm');
    if (!tabLogin) return;

    // 先全部隐藏
    loginForm.style.display = 'none';
    phoneLoginForm.style.display = 'none';
    registerForm.style.display = 'none';
    tabLogin.classList.remove('active');
    tabPhone.classList.remove('active');
    tabRegister.classList.remove('active');

    // 显示目标 tab
    if (tab === 'login') {
        tabLogin.classList.add('active');
        loginForm.style.display = 'flex';
    } else if (tab === 'phone') {
        tabPhone.classList.add('active');
        phoneLoginForm.style.display = 'flex';
    } else {
        tabRegister.classList.add('active');
        registerForm.style.display = 'flex';
    }
    hideError('loginError');
    hideError('phoneLoginError');
    hideError('regError');
}

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
            // 登录成功，跳转到首页
            window.location.href = '/';
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

// ===== 手机号登录 =====

async function doPhoneLogin() {
    const phone = document.getElementById('phoneLoginPhone').value.trim();
    const code = document.getElementById('phoneLoginCode').value.trim();

    if (!phone || !/^1[3-9]\d{9}$/.test(phone)) { showError('phoneLoginError', '请输入正确的手机号'); return; }
    if (!code || code.length < 4) { showError('phoneLoginError', '请输入验证码'); return; }

    const btn = document.querySelector('#phoneLoginForm .auth-submit');
    btn.disabled = true;
    btn.textContent = '登录中...';
    hideError('phoneLoginError');

    try {
        const resp = await fetch('/api/auth/login/phone', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ phone, code })
        });
        const data = await resp.json();

        if (data.success) {
            window.location.href = '/';
        } else {
            showError('phoneLoginError', data.error || data.message || '登录失败');
        }
    } catch (err) {
        showError('phoneLoginError', '网络异常，请稍后重试');
    } finally {
        btn.disabled = false;
        btn.textContent = '登 录';
    }
}

// ===== 发送短信验证码 =====

async function sendSmsCode(btnId, phoneInputId) {
    const phone = document.getElementById(phoneInputId).value.trim();
    if (!phone || !/^1[3-9]\d{9}$/.test(phone)) {
        // 根据按钮所在位置显示对应错误提示
        const errorId = phoneInputId === 'regPhone' ? 'regError' : 'phoneLoginError';
        showError(errorId, '请输入正确的手机号');
        return;
    }

    const btn = document.getElementById(btnId);
    btn.disabled = true;
    btn.textContent = '发送中...';

    try {
        const resp = await fetch('/api/auth/sms/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ phone })
        });
        const data = await resp.json();

        if (data.success) {
            // 开始倒计时
            let seconds = 60;
            btn.textContent = seconds + 's';
            const timer = setInterval(() => {
                seconds--;
                if (seconds <= 0) {
                    clearInterval(timer);
                    btn.disabled = false;
                    btn.textContent = '获取验证码';
                } else {
                    btn.textContent = seconds + 's';
                }
            }, 1000);
        } else {
            const errorId = phoneInputId === 'regPhone' ? 'regError' : 'phoneLoginError';
            showError(errorId, data.message || '验证码发送失败');
            btn.disabled = false;
            btn.textContent = '获取验证码';
        }
    } catch (err) {
        const errorId = phoneInputId === 'regPhone' ? 'regError' : 'phoneLoginError';
        showError(errorId, '网络异常，请稍后重试');
        btn.disabled = false;
        btn.textContent = '获取验证码';
    }
}

// ===== 注册 =====

async function doRegister() {
    const username = document.getElementById('regUsername').value.trim();
    const nickname = document.getElementById('regNickname').value.trim();
    const password = document.getElementById('regPassword').value;
    const password2 = document.getElementById('regPassword2').value;
    const phone = document.getElementById('regPhone').value.trim();
    const smsCode = document.getElementById('regSmsCode').value.trim();

    if (!username || username.length < 3) { showError('regError', '用户名至少3位'); return; }
    if (!password || password.length < 6) { showError('regError', '密码至少6位'); return; }
    if (password !== password2) { showError('regError', '两次密码不一致'); return; }

    // 如果填写了手机号，必须填写验证码
    if (phone && !/^1[3-9]\d{9}$/.test(phone)) { showError('regError', '请输入正确的手机号'); return; }
    if (phone && (!smsCode || smsCode.length < 4)) { showError('regError', '请填写短信验证码'); return; }

    const btn = document.querySelector('#registerForm .auth-submit');
    btn.disabled = true;
    btn.textContent = '注册中...';
    hideError('regError');

    try {
        const payload = { username, password };
        if (nickname) payload.nickname = nickname;
        if (phone) {
            payload.phone = phone;
            payload.smsCode = smsCode;
        }

        const resp = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await resp.json();

        if (data.success) {
            // 注册成功，自动切换到登录并填充用户名
            switchAuthTab('login');
            document.getElementById('loginUsername').value = username;
            document.getElementById('loginPassword').value = '';
            document.getElementById('loginPassword').focus();
            // 清空注册表单
            document.getElementById('regUsername').value = '';
            document.getElementById('regNickname').value = '';
            document.getElementById('regPassword').value = '';
            document.getElementById('regPassword2').value = '';
            document.getElementById('regPhone').value = '';
            document.getElementById('regSmsCode').value = '';
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
    // 检查 URL 参数，如果有 tab 参数则切换到对应 tab
    const urlParams = new URLSearchParams(window.location.search);
    const tabParam = urlParams.get('tab');
    if (tabParam === 'register') {
        switchAuthTab('register');
    } else if (tabParam === 'phone') {
        switchAuthTab('phone');
    } else {
        switchAuthTab('login');
    }

    // 登录表单回车
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') doLogin();
        });
    }
    // 手机号登录表单回车
    const phoneLoginForm = document.getElementById('phoneLoginForm');
    if (phoneLoginForm) {
        phoneLoginForm.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') doPhoneLogin();
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
