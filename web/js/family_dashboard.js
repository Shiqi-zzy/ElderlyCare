/* 智慧养老平台 - 家属端控制台 */
let currentElderlyId = null;

// 初始化家属端
async function initFamilyDashboard() {
    await loadElderlyList();
    setupFamilyNav();
}

// 加载老人列表
async function loadElderlyList() {
    try {
        const data = await api.getMyElderly();
        const listEl = document.getElementById('elderly-list');
        const emptyEl = document.getElementById('elderly-empty');

        if (data.total === 0) {
            listEl.innerHTML = '';
            emptyEl.classList.remove('hidden');
            return;
        }

        emptyEl.classList.add('hidden');
        listEl.innerHTML = data.items.map(e => `
            <div class="elderly-card card" onclick="selectElderly('${e.id}')" id="elderly-${e.id}">
                <div class="flex-between">
                    <div>
                        <strong>${e.name}</strong>
                        <span class="text-muted" style="margin-left:8px;font-size:13px;">${e.care_level}</span>
                    </div>
                    <span class="status-badge ${e.privacy_paused ? 'status-active' : 'status-resolved'}">
                        ${e.privacy_paused ? '监控已暂停' : '监控中'}
                    </span>
                </div>
                <div class="text-muted mt-8" style="font-size:13px;">
                    ${e.address} · ${e.phone}
                </div>
            </div>
        `).join('');

        // 默认选中第一个老人
        if (data.items.length > 0) {
            selectElderly(data.items[0].id);
        }
    } catch (e) {
        showToast('加载老人列表失败: ' + e.message, 'error');
    }
}

// 选中老人，加载其告警和授权
async function selectElderly(elderlyId) {
    currentElderlyId = elderlyId;

    // 高亮选中卡片
    document.querySelectorAll('.elderly-card').forEach(c => c.style.border = '1px solid var(--border)');
    const card = document.getElementById(`elderly-${elderlyId}`);
    if (card) card.style.border = '2px solid var(--primary)';

    // 加载告警列表
    await loadAlarms(elderlyId);

    // 加载授权列表
    await loadAuthorizations(elderlyId);

    // 加载隐私状态
    await loadPrivacyStatus(elderlyId);

    // 预加载健康档案
    loadFamilyHealthRecords();
}

// 加载告警列表
async function loadAlarms(elderlyId) {
    const container = document.getElementById('alarm-list');
    try {
        const data = await api.getAlarms(elderlyId);
        if (data.total === 0) {
            container.innerHTML = '<div class="text-muted" style="padding:20px;text-align:center;">暂无告警记录</div>';
            return;
        }
        container.innerHTML = data.items.map(a => `
            <div class="alarm-item" style="padding:12px 0;border-bottom:1px solid var(--border);">
                <div class="flex-between">
                    <div>
                        <span class="level-badge level-${a.alarm_level}">${getAlarmLevelLabel(a.alarm_level)}</span>
                        <span style="margin-left:8px;font-weight:500;">${a.title}</span>
                    </div>
                    <span class="status-badge status-${a.status}">${getStatusLabel(a.status)}</span>
                </div>
                <div class="flex-between mt-8">
                    <span class="text-muted" style="font-size:12px;">${a.created_at} · AI置信度: ${a.ai_score ? (a.ai_score*100).toFixed(0)+'%' : 'N/A'}</span>
                    <div class="flex gap-8">
                        ${a.status === 'active' ? `<button class="btn btn-primary btn-sm" onclick="ackAlarm('${a.id}')">确认</button>` : ''}
                        <button class="btn btn-outline btn-sm" onclick="showAlarmDetail('${a.id}')">详情</button>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = `<div class="text-danger">加载告警失败: ${e.message}</div>`;
    }
}

// 确认告警
async function ackAlarm(alarmId) {
    try {
        await api.acknowledgeAlarm(alarmId);
        showToast('告警已确认', 'success');
        if (currentElderlyId) await loadAlarms(currentElderlyId);
    } catch (e) {
        showToast('确认告警失败: ' + e.message, 'error');
    }
}

// 模拟触发告警
async function simulateAlarm() {
    if (!currentElderlyId) {
        showToast('请先选择老人', 'error');
        return;
    }
    try {
        const result = await api.simulateAlarm();
        showToast(`模拟告警已触发: ${result.alarm.alarm_level} - ${result.alarm.title}`, 'info');
        await loadAlarms(currentElderlyId);
    } catch (e) {
        showToast('模拟告警失败: ' + e.message, 'error');
    }
}

// 查看告警详情
async function showAlarmDetail(alarmId) {
    try {
        const alarm = await api.getAlarmDetail(alarmId);
        const modal = document.getElementById('modal-container');
        modal.innerHTML = `
            <div class="modal-overlay" onclick="closeModal()">
                <div class="modal" onclick="event.stopPropagation()">
                    <h3>告警详情</h3>
                    <div style="margin-bottom:12px;">
                        <span class="level-badge level-${alarm.alarm_level}">${getAlarmLevelLabel(alarm.alarm_level)}</span>
                        <span class="status-badge status-${alarm.status}" style="margin-left:8px;">${getStatusLabel(alarm.status)}</span>
                    </div>
                    <p><strong>类型:</strong> ${alarm.alarm_type}</p>
                    <p><strong>标题:</strong> ${alarm.title}</p>
                    <p><strong>描述:</strong> ${alarm.description || '无'}</p>
                    <p><strong>AI置信度:</strong> ${alarm.ai_score ? (alarm.ai_score*100).toFixed(0)+'%' : 'N/A'}</p>
                    <p><strong>AI校验级别:</strong> ${alarm.ai_verified}</p>
                    <p><strong>创建时间:</strong> ${alarm.created_at}</p>
                    ${alarm.acknowledged_at ? `<p><strong>确认时间:</strong> ${alarm.acknowledged_at}</p>` : ''}
                    ${alarm.resolved_at ? `<p><strong>解决时间:</strong> ${alarm.resolved_at}</p>` : ''}
                    ${alarm.resolution_note ? `<p><strong>解决备注:</strong> ${alarm.resolution_note}</p>` : ''}
                    ${alarm.related_work_order_id ? `<p><strong>关联工单:</strong> ${alarm.related_work_order_id}</p>` : ''}
                    <div class="flex gap-8 mt-16" style="justify-content:flex-end;">
                        <button class="btn btn-outline" onclick="closeModal()">关闭</button>
                    </div>
                </div>
            </div>
        `;
    } catch (e) {
        showToast('获取告警详情失败: ' + e.message, 'error');
    }
}

function closeModal() {
    document.getElementById('modal-container').innerHTML = '';
}

// ── 授权管理 ──
async function loadAuthorizations(elderlyId) {
    const container = document.getElementById('auth-list');
    try {
        const data = await api.listAuthorizations(elderlyId);
        if (data.total === 0) {
            container.innerHTML = '<div class="text-muted" style="padding:10px;text-align:center;">暂无有效授权</div>';
            return;
        }
        container.innerHTML = data.items.map(a => `
            <div style="padding:8px 0;border-bottom:1px solid var(--border);">
                <div class="flex-between">
                    <div>
                        <strong>${a.grantee_name}</strong>
                        <span class="text-muted" style="margin-left:8px;font-size:12px;">${a.grantee_phone}</span>
                    </div>
                    <span class="status-badge ${a.status === 'active' ? 'status-resolved' : 'status-archived'}">${a.status}</span>
                </div>
                <div class="flex-between mt-8">
                    <span class="text-muted" style="font-size:12px;">${a.permission_type} · 截止: ${a.effective_until}</span>
                    ${a.status === 'active' ? `<button class="btn btn-danger btn-sm" onclick="revokeAuth('${a.id}')">撤销</button>` : ''}
                </div>
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = `<div class="text-danger">加载授权失败: ${e.message}</div>`;
    }
}

async function revokeAuth(authId) {
    if (!confirm('确认撤销此授权？撤销后对方将无法访问老人数据。')) return;
    try {
        await api.revokeAuthorization(authId);
        showToast('授权已撤销', 'success');
        if (currentElderlyId) await loadAuthorizations(currentElderlyId);
    } catch (e) {
        showToast('撤销失败: ' + e.message, 'error');
    }
}

function showGrantAuthModal() {
    if (!currentElderlyId) {
        showToast('请先选择老人', 'error');
        return;
    }
    const modal = document.getElementById('modal-container');
    modal.innerHTML = `
        <div class="modal-overlay" onclick="closeModal()">
            <div class="modal" onclick="event.stopPropagation()">
                <h3>授权数据访问</h3>
                <div class="form-group">
                    <label>被授权人ID (社区/医院人员)</label>
                    <input type="text" id="auth-grantee-id" placeholder="输入对方的用户ID">
                </div>
                <div class="form-group">
                    <label>权限类型</label>
                    <select id="auth-permission-type">
                        <option value="alarm_video">告警短视频</option>
                        <option value="monitoring">实时监控</option>
                        <option value="health_records">健康档案</option>
                        <option value="all">全部</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>数据范围 (JSON)</label>
                    <input type="text" id="auth-data-scope" value='{"video":true,"medical":false,"alarm":true}'>
                </div>
                <div class="form-group">
                    <label>有效期至</label>
                    <input type="text" id="auth-until" placeholder="2025-12-31 23:59:59">
                </div>
                <div class="flex gap-8" style="justify-content:flex-end;">
                    <button class="btn btn-outline" onclick="closeModal()">取消</button>
                    <button class="btn btn-primary" onclick="submitGrantAuth()">确认授权</button>
                </div>
            </div>
        </div>
    `;
}

async function submitGrantAuth() {
    const data = {
        elderly_id: currentElderlyId,
        grantee_user_id: document.getElementById('auth-grantee-id').value,
        permission_type: document.getElementById('auth-permission-type').value,
        data_scope: document.getElementById('auth-data-scope').value,
        effective_until: document.getElementById('auth-until').value
    };
    try {
        await api.grantAuthorization(data);
        showToast('授权成功', 'success');
        closeModal();
        await loadAuthorizations(currentElderlyId);
    } catch (e) {
        showToast('授权失败: ' + e.message, 'error');
    }
}

// ── 隐私控制 ──
async function loadPrivacyStatus(elderlyId) {
    try {
        const data = await api.getPrivacyStatus(elderlyId);
        const btn = document.getElementById('privacy-toggle-btn');
        if (data.privacy_paused) {
            btn.textContent = '恢复监控';
            btn.className = 'btn btn-outline btn-sm';
            btn.onclick = () => resumeMonitoring(elderlyId);
        } else {
            btn.textContent = '暂停监控';
            btn.className = 'btn btn-danger btn-sm';
            btn.onclick = () => pauseMonitoring(elderlyId);
        }
    } catch (e) { /* 忽略 */ }
}

async function pauseMonitoring(elderlyId) {
    try {
        await api.pauseMonitoring(elderlyId);
        showToast('监控已暂停（老人隐私保护）', 'info');
        await loadPrivacyStatus(elderlyId);
    } catch (e) {
        showToast('操作失败: ' + e.message, 'error');
    }
}

async function resumeMonitoring(elderlyId) {
    try {
        await api.resumeMonitoring(elderlyId);
        showToast('监控已恢复', 'success');
        await loadPrivacyStatus(elderlyId);
    } catch (e) {
        showToast('操作失败: ' + e.message, 'error');
    }
}

// ── 导航 ──
function setupFamilyNav() {
    document.querySelectorAll('.family-nav-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.family-nav-btn').forEach(b => b.style.borderBottom = 'none');
            btn.style.borderBottom = '2px solid var(--primary)';

            document.querySelectorAll('.family-page-section').forEach(s => s.classList.add('hidden'));
            const target = document.getElementById(btn.dataset.target);
            if (target) {
                target.classList.remove('hidden');
                // 切换到健康档案时自动加载
                if (btn.dataset.target === 'family-health') {
                    loadFamilyHealthRecords();
                }
            }
        });
    });
}

// ── 辅助函数 ──
function getAlarmLevelLabel(level) {
    const labels = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险', EMERGENCY: '紧急' };
    return labels[level] || level;
}

function getStatusLabel(status) {
    const labels = { active: '活跃', acknowledged: '已确认', processing: '处理中', resolved: '已解决', archived: '已归档' };
    return labels[status] || status;
}

// ── 社区端仪表盘 ──
async function initCommunityDashboard() {
    try {
        const dash = await api.getCommunityDashboard();
        document.getElementById('community-stats').innerHTML = `
            <div class="stat-card"><div class="stat-value">${dash.pending_work_orders}</div><div class="stat-label">待处理工单</div></div>
        `;
        const elderlyData = await api.getCommunityElderlyList();
        const elderlyList = elderlyData.items || [];
        document.getElementById('community-elderly-list').innerHTML = elderlyList.length === 0
            ? '<tr><td colspan="4" class="text-muted">暂未绑定老人</td></tr>'
            : elderlyList.map(e => `<tr><td>${e.name}</td><td>${e.gender}</td><td>${e.care_level}</td><td>${e.address}</td></tr>`).join('');

        // 填充设备管理老人选择器
        document.getElementById('community-device-elderly-select').innerHTML =
            '<option value="">-- 请选择 --</option>' + elderlyList.map(e => `<option value="${e.id}">${e.name} (${e.care_level})</option>`).join('');

        // 加载工单
        const orders = await api.getMyWorkOrders();
        document.getElementById('community-order-list').innerHTML = (orders.items || []).length === 0
            ? '<tr><td colspan="4" class="text-muted">暂无工单</td></tr>'
            : orders.items.map(o => `
                <tr>
                    <td>${o.title}</td><td>${o.priority}</td><td><span class="status-badge status-${o.status === 'pending' ? 'active' : 'resolved'}">${o.status}</span></td>
                    <td>${o.status === 'pending' ? `<button class="btn btn-primary btn-sm" onclick="acceptOrder('${o.id}')">接单</button>` : ''}</td>
                </tr>
            `).join('');
    } catch (e) {
        console.error(e);
    }
}

async function acceptOrder(orderId) {
    try {
        await api.acceptWorkOrder(orderId);
        showToast('已接单', 'success');
        initCommunityDashboard();
    } catch (e) {
        showToast('接单失败: ' + e.message, 'error');
    }
}

// ── 医院端仪表盘 ──
let hospitalElderlyList = [];

async function initHospitalDashboard() {
    try {
        const dash = await api.getHospitalDashboard();
        document.getElementById('hospital-stats').innerHTML = `
            <div class="stat-card"><div class="stat-value">${dash.bound_elderly_count}</div><div class="stat-label">绑定老人</div></div>
        `;

        // 加载绑定老人列表
        const elderlyData = await api.getHospitalElderlyList();
        hospitalElderlyList = elderlyData.items || [];
        document.getElementById('hospital-elderly-list').innerHTML = hospitalElderlyList.length === 0
            ? '<div class="text-muted" style="text-align:center;padding:20px;">暂无绑定老人</div>'
            : hospitalElderlyList.map(e => `
                <div style="padding:8px 0;border-bottom:1px solid var(--border);">
                    <strong>${e.name}</strong>
                    <span class="text-muted" style="margin-left:8px;font-size:12px;">${e.gender} · ${e.care_level} · ${e.address}</span>
                </div>
            `).join('');

        // 填充下拉选择器
        const elderlyOptions = hospitalElderlyList.map(e => `<option value="${e.id}">${e.name} (${e.care_level})</option>`).join('');
        document.getElementById('emergency-elderly-select').innerHTML = '<option value="">-- 请选择 --</option>' + elderlyOptions;
        document.getElementById('health-elderly-select').innerHTML = '<option value="">-- 请选择 --</option>' + elderlyOptions;

        // 加载急救状态
        await loadEmergencyStatus();

        // 加载设备列表（第一个老人默认）
        if (hospitalElderlyList.length > 0) {
            await loadHospitalDevices(hospitalElderlyList[0].id);
        }
    } catch (e) {
        console.error(e);
    }
}

// ── 急救权限 ──
async function loadEmergencyStatus() {
    try {
        const status = await api.getEmergencyStatus();
        const container = document.getElementById('hospital-emergency-status');
        if (status.active) {
            container.innerHTML = `
                <div style="background:rgba(34,197,94,0.1);padding:12px;border-radius:8px;">
                    <span style="color:#22c55e;font-weight:600;">✅ 急救权限已激活</span>
                    <span class="text-muted" style="margin-left:8px;">👴 ${status.elderly_name || '老人'} · 有效期至 ${(status.expires_at || '').replace('T', ' ').substring(0, 16)}</span>
                    <div class="text-muted" style="font-size:12px;margin-top:4px;">可临时查看监控视频、告警数据、健康档案</div>
                </div>`;
        } else {
            container.innerHTML = '<span class="text-muted">暂无活跃急救权限</span>';
        }
    } catch (e) {
        document.getElementById('hospital-emergency-status').innerHTML = '<span class="text-muted">加载失败</span>';
    }
}

async function requestEmergency() {
    const elderlyId = document.getElementById('emergency-elderly-select').value;
    const reason = document.getElementById('emergency-reason').value;
    if (!elderlyId) { showToast('请选择老人', 'error'); return; }
    if (!reason.trim()) { showToast('请填写急救理由', 'error'); return; }
    try {
        await api.requestEmergencyAccess(elderlyId, reason);
        showToast('急救权限请求已发送，家属端即时收到通知', 'success');
        document.getElementById('emergency-reason').value = '';
        await loadEmergencyStatus();
    } catch (e) {
        showToast('请求失败: ' + e.message, 'error');
    }
}

// ── 健康档案 ──
async function loadHospitalHealthRecords() {
    const elderlyId = document.getElementById('health-elderly-select').value;
    const container = document.getElementById('hospital-health-list');
    if (!elderlyId) {
        container.innerHTML = '<div class="text-muted" style="text-align:center;padding:20px;">请选择老人后查看健康档案</div>';
        return;
    }
    try {
        const data = await api.getHospitalHealthRecords(elderlyId);
        if (data.total === 0) {
            container.innerHTML = '<div class="text-muted" style="text-align:center;padding:20px;">暂无健康档案，点击"录入档案"按钮添加</div>';
            return;
        }
        container.innerHTML = data.items.map(r => `
            <div style="padding:12px 0;border-bottom:1px solid var(--border);">
                <div class="flex-between">
                    <div>
                        <span class="status-badge status-resolved">${r.record_type}</span>
                        <span style="margin-left:8px;font-size:13px;">${r.record_date}</span>
                        ${r.visibility === 'both' ? '<span class="text-success" style="margin-left:4px;font-size:11px;">家属可见</span>' : '<span class="text-muted" style="margin-left:4px;font-size:11px;">仅医院</span>'}
                    </div>
                </div>
                <div style="margin-top:6px;color:var(--text);">${r.content_json}</div>
                ${r.doctor_name || r.hospital_name ? `<div class="text-muted mt-8" style="font-size:11px;">👨‍⚕ ${r.doctor_name || ''} · 🏥 ${r.hospital_name || ''}</div>` : ''}
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = `<div class="text-danger">加载失败: ${e.message}</div>`;
    }
}

function showAddHealthRecordModal() {
    const elderlyId = document.getElementById('health-elderly-select').value;
    if (!elderlyId) { showToast('请先选择老人', 'error'); return; }
    const modal = document.getElementById('modal-container');
    modal.innerHTML = `
        <div class="modal-overlay" onclick="closeModal()">
            <div class="modal" onclick="event.stopPropagation()">
                <h3>录入健康档案</h3>
                <div class="form-group"><label>档案类型</label><select id="hr-type"><option value="诊断">诊断</option><option value="处方">处方</option><option value="检查报告">检查报告</option><option value="用药记录">用药记录</option><option value="疫苗接种">疫苗接种</option></select></div>
                <div class="form-group"><label>记录日期</label><input type="date" id="hr-date"></div>
                <div class="form-group"><label>医生姓名</label><input type="text" id="hr-doctor" placeholder="选填"></div>
                <div class="form-group"><label>医院名称</label><input type="text" id="hr-hospital" placeholder="选填"></div>
                <div class="form-group"><label>档案内容（JSON或自由文本）</label><textarea id="hr-content" rows="3" placeholder='如：{"诊断":"高血压2级","血压":"160/95","医嘱":"定期服药"}'>档案内容</textarea></div>
                <div class="form-group"><label>可见范围</label><select id="hr-visibility"><option value="both">家属可见</option><option value="hospital">仅医院</option></select></div>
                <div class="flex gap-8" style="justify-content:flex-end;">
                    <button class="btn btn-outline" onclick="closeModal()">取消</button>
                    <button class="btn btn-primary" onclick="submitHealthRecord('${elderlyId}')">保存</button>
                </div>
            </div>
        </div>
    `;
    document.getElementById('hr-date').value = new Date().toISOString().split('T')[0];
}

async function submitHealthRecord(elderlyId) {
    const data = {
        record_type: document.getElementById('hr-type').value,
        record_date: document.getElementById('hr-date').value,
        doctor_name: document.getElementById('hr-doctor').value,
        hospital_name: document.getElementById('hr-hospital').value,
        content_json: document.getElementById('hr-content').value,
        visibility: document.getElementById('hr-visibility').value
    };
    try {
        await api.addHealthRecord(elderlyId, data);
        showToast('健康档案已录入', 'success');
        closeModal();
        await loadHospitalHealthRecords();
    } catch (e) {
        showToast('录入失败: ' + e.message, 'error');
    }
}

// ── 医院设备绑定 ──
async function bindHospitalDevice() {
    const code = document.getElementById('hospital-device-code').value.trim();
    if (code.length !== 6) { showToast('请输入6位验证码', 'error'); return; }
    try {
        const result = await api.bindHospitalDevice(code);
        showToast(result.message || '设备绑定成功', 'success');
        document.getElementById('hospital-device-code').value = '';
        if (result.elderly_id) await loadHospitalDevices(result.elderly_id);
    } catch (e) {
        showToast('绑定失败: ' + e.message, 'error');
    }
}

async function loadHospitalDevices(elderlyId) {
    try {
        const data = await api.getHospitalDevices(elderlyId);
        const container = document.getElementById('hospital-device-list');
        if (!data.items || data.items.length === 0) {
            container.innerHTML = '<div class="text-muted" style="padding:8px;">暂无绑定设备</div>';
            return;
        }
        container.innerHTML = data.items.map(d => `
            <div style="padding:8px 0;border-bottom:1px solid var(--border);">
                <span>${d.device_type === 'camera' ? '📹' : d.device_type === 'wearable' ? '⌚' : d.device_type === 'bed_sensor' ? '🛏' : '📡'} ${d.device_name}</span>
                <span class="status-badge ${d.status === 'online' ? 'status-resolved' : 'status-archived'}" style="margin-left:8px;">${d.status === 'online' ? '在线' : '离线'}</span>
                <span class="text-muted" style="margin-left:8px;font-size:12px;">${d.device_type} · ${d.location || '-'}</span>
            </div>
        `).join('');
    } catch (e) {
        console.error(e);
    }
}

// ── 家属端健康档案 ──
async function loadFamilyHealthRecords() {
    if (!currentElderlyId) { return; }
    const container = document.getElementById('family-health-list');
    try {
        const data = await api.getFamilyHealthRecords(currentElderlyId);
        if (data.total === 0) {
            container.innerHTML = '<div class="text-muted" style="text-align:center;padding:20px;">暂无健康档案</div>';
            return;
        }
        container.innerHTML = data.items.map(r => `
            <div style="padding:12px 0;border-bottom:1px solid var(--border);">
                <div class="flex-between">
                    <div>
                        <span class="status-badge status-resolved">${r.record_type}</span>
                        <span style="margin-left:8px;font-size:13px;color:var(--text-secondary);">${r.record_date}</span>
                    </div>
                </div>
                <div style="margin-top:6px;">${r.content_json}</div>
                ${r.doctor_name || r.hospital_name ? `<div class="text-muted mt-8" style="font-size:11px;">👨‍⚕ ${r.doctor_name || ''} · 🏥 ${r.hospital_name || ''}</div>` : ''}
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = `<div class="text-danger">加载失败: ${e.message}</div>`;
    }
}

// ── 社区设备管理 ──
async function bindCommunityDevice() {
    const code = document.getElementById('community-device-code').value.trim();
    if (code.length !== 6) { showToast('请输入6位验证码', 'error'); return; }
    try {
        const result = await api.bindCommunityDevice(code);
        showToast(result.message || '设备绑定成功', 'success');
        document.getElementById('community-device-code').value = '';
        if (result.elderly_id) {
            document.getElementById('community-device-elderly-select').value = result.elderly_id;
            await loadCommunityDevices();
        }
    } catch (e) {
        showToast('绑定失败: ' + e.message, 'error');
    }
}

async function loadCommunityDevices() {
    const elderlyId = document.getElementById('community-device-elderly-select').value;
    const container = document.getElementById('community-device-list');
    if (!elderlyId) {
        container.innerHTML = '<div class="text-muted" style="padding:8px;">请选择老人</div>';
        return;
    }
    try {
        const data = await api.getCommunityDevices(elderlyId);
        if (!data.items || data.items.length === 0) {
            container.innerHTML = '<div class="text-muted" style="padding:8px;">暂无绑定设备</div>';
            return;
        }
        container.innerHTML = `<table class="data-table"><thead><tr><th>设备</th><th>类型</th><th>位置</th><th>状态</th><th>操作</th></tr></thead><tbody>
            ${data.items.map(d => `
                <tr>
                    <td>${d.device_name}</td><td>${d.device_type}</td><td>${d.location || '-'}</td>
                    <td><span class="status-badge ${d.status === 'online' ? 'status-resolved' : 'status-archived'}">${d.status === 'online' ? '在线' : '离线'}</span></td>
                    <td><button class="btn btn-outline btn-sm" onclick="showInspectionModal('${d.id}')">巡检</button></td>
                </tr>
            `).join('')}
        </tbody></table>`;
    } catch (e) {
        container.innerHTML = `<div class="text-danger">加载失败: ${e.message}</div>`;
    }
}

function showInspectionModal(deviceId) {
    const modal = document.getElementById('modal-container');
    modal.innerHTML = `
        <div class="modal-overlay" onclick="closeModal()">
            <div class="modal" onclick="event.stopPropagation()">
                <h3>设备巡检</h3>
                <div class="form-group"><label>巡检类型</label><select id="insp-type"><option value="routine">日常巡检</option><option value="repair">维修</option><option value="emergency">紧急检查</option></select></div>
                <div class="form-group"><label>巡检结果</label><select id="insp-status"><option value="normal">正常</option><option value="attention">需关注</option><option value="fault">故障</option></select></div>
                <div class="form-group"><label>发现情况</label><textarea id="insp-findings" rows="2" placeholder="选填"></textarea></div>
                <div class="flex gap-8" style="justify-content:flex-end;">
                    <button class="btn btn-outline" onclick="closeModal()">取消</button>
                    <button class="btn btn-primary" onclick="submitInspection('${deviceId}')">提交</button>
                </div>
            </div>
        </div>
    `;
}

async function submitInspection(deviceId) {
    const type = document.getElementById('insp-type').value;
    const status = document.getElementById('insp-status').value;
    const findings = document.getElementById('insp-findings').value;
    try {
        await api.logDeviceInspection(deviceId, type, status, findings);
        showToast('巡检记录已保存', 'success');
        closeModal();
    } catch (e) {
        showToast('巡检提交失败: ' + e.message, 'error');
    }
}

// ── 管理端仪表盘 Phase 4 ──
async function initAdminDashboard() {
    try {
        // Phase 4: 增强统计
        let stats;
        try {
            stats = await api.getStatisticsOverview();
        } catch (_) {
            // 降级到旧版统计接口
            stats = await api.getAdminStats();
        }

        document.getElementById('admin-stats').innerHTML = `
            <div class="stat-card"><div class="stat-value">${stats.total_users || 0}</div><div class="stat-label">活跃用户</div></div>
            <div class="stat-card"><div class="stat-value">${stats.total_elderly || 0}</div><div class="stat-label">在管老人</div></div>
            <div class="stat-card"><div class="stat-value">${stats.online_devices || 0}</div><div class="stat-label">在线设备</div></div>
            <div class="stat-card"><div class="stat-value">${stats.active_alarms || 0}</div><div class="stat-label">活跃告警</div></div>
            <div class="stat-card"><div class="stat-value">${stats.pending_work_orders || 0}</div><div class="stat-label">待处理工单</div></div>
            ${stats.total_health_records !== undefined ? `<div class="stat-card"><div class="stat-value">${stats.total_health_records}</div><div class="stat-label">健康档案</div></div>` : ''}
        `;

        // Phase 4: 告警趋势
        try {
            const trends = await api.getAlarmTrends(30);
            const dailyData = trends.daily_trend || {};
            const days = Object.keys(dailyData).sort().slice(-14); // 最近14天
            const maxVal = Math.max(1, ...days.map(d => {
                const v = dailyData[d]; return (v.LOW||0)+(v.MEDIUM||0)+(v.HIGH||0)+(v.EMERGENCY||0);
            }));

            document.getElementById('admin-alarm-trend').innerHTML = days.length === 0
                ? '<span class="text-muted">近30天暂无告警数据</span>'
                : `<div style="display:flex;align-items:end;gap:4px;height:150px;padding:0 8px;">
                    ${days.map(d => {
                        const v = dailyData[d];
                        const total = (v?.LOW||0)+(v?.MEDIUM||0)+(v?.HIGH||0)+(v?.EMERGENCY||0);
                        const h = Math.max(4, (total / maxVal) * 130);
                        return `<div style="flex:1;text-align:center;" title="${d}: ${total}条">
                            <div style="height:${h}px;background:var(--primary);border-radius:4px 4px 0 0;margin-bottom:4px;"></div>
                            <span style="font-size:9px;color:var(--text-muted);">${d.substring(5)}</span>
                            <span style="font-size:9px;color:var(--text-secondary);">${total}</span>
                        </div>`;
                    }).join('')}
                </div>`;

            // Top告警老人
            if (trends.top_elderly_by_alarms && trends.top_elderly_by_alarms.length > 0) {
                document.getElementById('admin-top-elderly').innerHTML = trends.top_elderly_by_alarms.map((e, i) => `
                    <div style="padding:4px 0;border-bottom:1px solid var(--border);display:flex;justify-content:space-between;">
                        <span>${i+1}. ${e.name} <span class="text-muted" style="font-size:11px;">${(e.elderly_id||'').substring(0,8)}...</span></span>
                        <span class="status-badge ${e.count > 10 ? 'status-active' : 'status-acknowledged'}">${e.count} 条</span>
                    </div>
                `).join('');
            } else {
                document.getElementById('admin-top-elderly').innerHTML = '<span class="text-muted">暂无数据</span>';
            }
        } catch (_) {
            document.getElementById('admin-alarm-trend').innerHTML = '<span class="text-muted">告警趋势加载失败</span>';
            document.getElementById('admin-top-elderly').innerHTML = '<span class="text-muted">—</span>';
        }

        // 审计日志
        const logs = await api.getAuditLogs({ limit: 20 });
        document.getElementById('admin-audit-list').innerHTML = (logs.items || []).map(l => `
            <tr>
                <td>${l.event_type}</td><td>${l.operator}</td><td>${l.target_type || '-'}</td>
                <td>${l.target_id ? l.target_id.substring(0,12)+'...' : '-'}</td><td>${l.created_at}</td>
            </tr>
        `).join('') || '<tr><td colspan="5" class="text-muted">暂无审计日志</td></tr>';
    } catch (e) {
        console.error(e);
    }
}

// Phase 4: AI健康报告查询
async function lookupHealthReport() {
    const elderlyId = document.getElementById('admin-health-elderly-id').value.trim();
    const container = document.getElementById('admin-health-report');
    if (!elderlyId) { showToast('请输入老人ID', 'error'); return; }

    try {
        const report = await api.getHealthReport(elderlyId);
        if (report.error) {
            container.innerHTML = `<div class="text-danger">${report.error}</div>`;
            return;
        }
        const fr = report.fall_risk || {};
        const ht = report.health_trend || {};
        const dev = report.device_summary || {};

        container.innerHTML = `
            <div style="background:var(--bg);padding:16px;border-radius:8px;">
                <h4>👴 ${report.elderly_name} · ${report.care_level}</h4>
                <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:12px;">
                    <div>
                        <strong>跌倒风险:</strong>
                        <span class="status-badge ${fr.risk_level_code === 'high' ? 'status-active' : fr.risk_level_code === 'medium' ? 'status-acknowledged' : 'status-resolved'}">
                            ${fr.risk_level || '—'} (${fr.risk_score || 0}分)
                        </span>
                    </div>
                    <div><strong>健康趋势:</strong> ${ht.trend || '—'}</div>
                    <div><strong>近30天档案:</strong> ${ht.record_count || 0} 条</div>
                    <div><strong>设备:</strong> ${dev.online || 0}/${dev.total || 0} 在线</div>
                </div>
                ${fr.factors ? `
                <div class="mt-8"><strong>风险因子:</strong>
                    ${fr.factors.filter(f => f.score > 0).map(f => `<div style="font-size:12px;color:var(--text-secondary);">· ${f.factor}: +${f.score}分 (${f.detail})</div>`).join('') || '<span class="text-muted">无显著风险因子</span>'}
                </div>` : ''}
            </div>`;
    } catch (e) {
        container.innerHTML = `<div class="text-danger">查询失败: ${e.message}</div>`;
    }
}
