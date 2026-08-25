/* 智慧养老平台 - 认证模块 */
let currentUser = null;

// 检查登录状态，如未登录则显示登录页
function checkAuth() {
    const token = localStorage.getItem('access_token');
    if (!token) {
        showLoginPage();
        return false;
    }
    return true;
}

// 获取当前用户信息
async function loadCurrentUser() {
    try {
        currentUser = await api.getMe();
        return currentUser;
    } catch (e) {
        console.error('获取用户信息失败:', e);
        api.clearToken();
        showLoginPage();
        return null;
    }
}

// 获取验证码
async function handleSendCode() {
    const phone = document.getElementById('login-phone').value;
    if (!phone || phone.length !== 11) {
        showToast('请先输入 11 位手机号', 'error');
        return;
    }
    const btn = document.getElementById('send-code-btn');
    try {
        await api.sendCode(phone);
        showToast('验证码已发送（短信不可用时固定 123456）', 'success');
        let left = 60;
        btn.disabled = true;
        const timer = setInterval(() => {
            btn.textContent = `重新获取(${left}s)`;
            if (--left <= 0) {
                clearInterval(timer);
                btn.disabled = false;
                btn.textContent = '获取验证码';
            }
        }, 1000);
    } catch (e) {
        showToast('发送失败: ' + e.message, 'error');
    }
}

// 登录处理
async function handleLogin(event) {
    event.preventDefault();
    const phone = document.getElementById('login-phone').value;
    const code = document.getElementById('login-code').value;
    const role = document.getElementById('login-role').value;

    try {
        const result = await api.login(phone, code, role);
        currentUser = result.user;
        showToast('登录成功', 'success');
        initPortalByRole(currentUser.role);
    } catch (e) {
        showToast('登录失败: ' + e.message, 'error');
    }
}

// 退出登录
function logout() {
    api.clearToken();
    currentUser = null;
    showLoginPage();
}

// 显示登录页
function showLoginPage() {
    document.getElementById('login-page').classList.remove('hidden');
    document.getElementById('app-container').classList.add('hidden');
}

// 显示应用
function showApp() {
    document.getElementById('login-page').classList.add('hidden');
    document.getElementById('app-container').classList.remove('hidden');
}
