/**
 * 微信扫码登录 - 前端认证逻辑
 *
 * 功能：
 * 1. 检查登录状态 → 已登录直接跳转 about.html
 * 2. 未登录 → 弹出微信扫码登录窗口
 * 3. 点击登录 → 获取微信授权 URL → 跳转微信扫码页
 * 4. 扫码成功后微信回调 → 自动跳转 about.html
 */

// ===== 登录弹窗控制 =====

function showLoginModal() {
    const overlay = document.getElementById('loginOverlay');
    overlay.style.display = 'flex';
    // 重置状态
    document.getElementById('wechatLoginBtn').style.display = 'flex';
    document.getElementById('loginLoading').style.display = 'none';
    document.getElementById('loginError').style.display = 'none';
}

function closeLoginModal() {
    document.getElementById('loginOverlay').style.display = 'none';
}

// 点击遮罩层关闭弹窗
document.addEventListener('DOMContentLoaded', () => {
    const overlay = document.getElementById('loginOverlay');
    if (overlay) {
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) closeLoginModal();
        });
    }
});

// ESC 关闭弹窗
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeLoginModal();
});

// ===== 核心：检查登录状态并跳转 =====

async function checkAndGotoAbout() {
    try {
        const resp = await fetch('/api/auth/status');
        const data = await resp.json();

        if (data.loggedIn) {
            // 已登录，直接跳转
            window.location.href = '/about.html';
        } else {
            // 未登录，显示登录弹窗
            showLoginModal();
        }
    } catch (err) {
        console.error('检查登录状态失败:', err);
        // 网络异常时也弹出登录窗口
        showLoginModal();
    }
}

// ===== 发起微信扫码登录 =====

async function startWechatLogin() {
    const loginBtn = document.getElementById('wechatLoginBtn');
    const loading = document.getElementById('loginLoading');
    const errorDiv = document.getElementById('loginError');

    // 切换到加载状态
    loginBtn.style.display = 'none';
    loading.style.display = 'block';
    errorDiv.style.display = 'none';

    try {
        const resp = await fetch('/auth/wechat/login');
        const data = await resp.json();

        if (data.success && data.authorizeUrl) {
            // 跳转到微信扫码授权页面
            window.location.href = data.authorizeUrl;
        } else {
            throw new Error(data.message || '获取授权链接失败');
        }
    } catch (err) {
        console.error('获取微信授权 URL 失败:', err);
        loading.style.display = 'none';
        loginBtn.style.display = 'flex';
        errorDiv.style.display = 'block';
        errorDiv.textContent = '⚠️ ' + (err.message || '网络异常，请稍后重试');
    }
}
