/* 智慧养老平台 - 端口路由：按角色加载对应界面 */
const ROLE_PAGES = {
    family: 'family_page',
    community: 'community_page',
    hospital: 'hospital_page',
    admin: 'admin_page'
};

// 按角色初始化门户
async function initPortalByRole(role) {
    showApp();

    // 隐藏所有端口页
    Object.values(ROLE_PAGES).forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.add('hidden');
    });

    // 显示对应端口页
    const pageId = ROLE_PAGES[role];
    const page = document.getElementById(pageId);
    if (page) page.classList.remove('hidden');

    // 更新顶部导航
    document.getElementById('topbar-username').textContent = currentUser.real_name;
    const roleBadge = document.getElementById('topbar-role');
    roleBadge.textContent = getRoleLabel(role);
    roleBadge.className = `role-badge role-${role}`;

    // 按角色初始化
    switch (role) {
        case 'family':
            await initFamilyDashboard();
            break;
        case 'community':
            await initCommunityDashboard();
            break;
        case 'hospital':
            await initHospitalDashboard();
            break;
        case 'admin':
            await initAdminDashboard();
            break;
    }
}

function getRoleLabel(role) {
    const labels = { family: '家属', community: '社区', hospital: '医院', admin: '管理员' };
    return labels[role] || role;
}

// Toast 通知
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}
