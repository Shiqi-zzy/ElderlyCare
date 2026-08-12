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

// 登录处理
async function handleLogin(event) {
    event.preventDefault();
    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;

    try {
        const result = await api.login(username, password);
        currentUser = result.user;
        showToast('登录成功', 'success');
        initPortalByRole(currentUser.role);
    } catch (e) {
        showToast('登录失败: ' + e.message, 'error');
    }
}

// 注册处理
async function handleRegister(event) {
    event.preventDefault();
    const username = document.getElementById('reg-username').value;
    const password = document.getElementById('reg-password').value;
    const realName = document.getElementById('reg-realname').value;
    const phone = document.getElementById('reg-phone').value;
    const role = document.getElementById('reg-role').value;

    try {
        const result = await api.register(username, password, realName, phone, role);
        currentUser = result.user;
        showToast('注册成功', 'success');
        initPortalByRole(currentUser.role);
    } catch (e) {
        showToast('注册失败: ' + e.message, 'error');
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
