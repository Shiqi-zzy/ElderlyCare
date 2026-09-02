/* 智慧养老平台 - API 通信层 */
const API_BASE = 'http://localhost:8002';

class ApiClient {
    constructor() {
        this.token = localStorage.getItem('access_token');
    }

    setToken(token) {
        this.token = token;
        localStorage.setItem('access_token', token);
    }

    clearToken() {
        this.token = null;
        localStorage.removeItem('access_token');
    }

    async request(method, path, body = null) {
        const headers = { 'Content-Type': 'application/json' };
        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        const opts = { method, headers };
        if (body) {
            opts.body = JSON.stringify(body);
        }

        const resp = await fetch(`${API_BASE}${path}`, opts);
        const data = await resp.json();

        if (!resp.ok) {
            const detail = data.detail || resp.statusText;
            throw new Error(detail);
        }

        return data;
    }

    get(path) { return this.request('GET', path); }
    post(path, body) { return this.request('POST', path, body); }
    put(path, body) { return this.request('PUT', path, body); }

    // ── 认证 ──
    // 平台后端登录契约：手机验证码登录（P1 短信不可用时固定验证码 123456），
    // 首次登录自动创建对应角色账号，无需单独注册
    async sendCode(phone) {
        return this.post('/api/auth/send-code', { phone });
    }

    async login(phone, code, role) {
        const result = await this.post('/api/auth/login', { phone, code, role });
        this.setToken(result.access_token);
        return result;
    }

    async getMe() {
        return this.get('/api/auth/me');
    }

    // ── 老人档案 ──
    async getMyElderly() {
        return this.get('/api/family/elderly/list');
    }

    async getElderly(id) {
        return this.get(`/api/family/elderly/${id}`);
    }

    async createElderly(data) {
        return this.post('/api/family/elderly', data);
    }

    // ── 告警 ──
    async getAlarms(elderlyId, level = null, status = null) {
        let path = `/api/family/alarms/${elderlyId}?`;
        if (level) path += `level=${level}&`;
        if (status) path += `status=${status}&`;
        return this.get(path);
    }

    async getAlarmDetail(alarmId) {
        return this.get(`/api/family/alarm/${alarmId}`);
    }

    async acknowledgeAlarm(alarmId) {
        return this.post(`/api/family/alarm/${alarmId}/acknowledge`);
    }

    async simulateAlarm() {
        return this.post('/api/alarm/simulate/trigger');
    }

    // ── 授权 ──
    async grantAuthorization(data) {
        return this.post('/api/family/authorization/grant', data);
    }

    async revokeAuthorization(authId, reason = null) {
        return this.post(`/api/family/authorization/revoke/${authId}`, { revoke_reason: reason });
    }

    async listAuthorizations(elderlyId) {
        return this.get(`/api/family/authorization/list/${elderlyId}`);
    }

    // ── 隐私控制 ──
    async pauseMonitoring(elderlyId) {
        return this.post(`/api/family/monitoring/pause/${elderlyId}`);
    }

    async resumeMonitoring(elderlyId) {
        return this.post(`/api/family/monitoring/resume/${elderlyId}`);
    }

    async getPrivacyStatus(elderlyId) {
        return this.get(`/api/family/privacy/status/${elderlyId}`);
    }

    // ── 工单 ──
    async getMyWorkOrders(status = null) {
        let path = '/api/work_order/my?';
        if (status) path += `status=${status}&`;
        return this.get(path);
    }

    async acceptWorkOrder(orderId) {
        return this.post(`/api/work_order/${orderId}/accept`);
    }

    async completeWorkOrder(orderId, resultJson, photos) {
        return this.post(`/api/work_order/${orderId}/complete`, {
            result_json: JSON.stringify(resultJson),
            result_photos: photos
        });
    }

    // ── 管理 ──
    async getAdminStats() {
        return this.get('/api/admin/stats');
    }

    async getAuditLogs(filters = {}) {
        let path = '/api/admin/audit/logs?';
        Object.entries(filters).forEach(([k, v]) => { if (v) path += `${k}=${v}&`; });
        return this.get(path);
    }

    // ── 社区 ──
    async getCommunityDashboard() {
        return this.get('/api/community/dashboard');
    }

    async getCommunityElderlyList() {
        return this.get('/api/community/elderly/list');
    }

    async bindCommunityDevice(code) {
        return this.post('/api/community/device/bind', { code });
    }

    async getCommunityDevices(elderlyId) {
        return this.get(`/api/community/devices/${elderlyId}`);
    }

    async logDeviceInspection(deviceId, type, status, findings = '', photos = '') {
        return this.post(`/api/community/device/${deviceId}/inspection`, {
            inspection_type: type, status, findings, photos
        });
    }

    async getDeviceMaintenanceHistory(deviceId) {
        return this.get(`/api/community/device/${deviceId}/maintenance`);
    }

    // ── 医院 ──
    async getHospitalDashboard() {
        return this.get('/api/hospital/dashboard');
    }

    async getHospitalElderlyList() {
        return this.get('/api/hospital/elderly/list');
    }

    async bindHospitalDevice(code) {
        return this.post('/api/hospital/device/bind', { code });
    }

    async getHospitalDevices(elderlyId) {
        return this.get(`/api/hospital/devices/${elderlyId}`);
    }

    // ── 健康档案（Phase 3） ──
    async addHealthRecord(elderlyId, data) {
        return this.post(`/api/hospital/health/${elderlyId}/add`, data);
    }

    async getHospitalHealthRecords(elderlyId) {
        return this.get(`/api/hospital/health/${elderlyId}`);
    }

    async getFamilyHealthRecords(elderlyId) {
        return this.get(`/api/family/health/${elderlyId}`);
    }

    // ── 急救权限（Phase 3） ──
    async requestEmergencyAccess(elderlyId, reason) {
        return this.post('/api/hospital/emergency/request', { elderly_id: elderlyId, reason });
    }

    async getEmergencyStatus() {
        return this.get('/api/hospital/emergency/status');
    }

    // ── 授权申请审批 ──
    async getPendingAuthorizationRequests() {
        return this.get('/api/family/authorization/requests');
    }

    async approveAuthorizationRequest(requestId) {
        return this.post(`/api/family/authorization/requests/${requestId}/approve`);
    }

    async rejectAuthorizationRequest(requestId) {
        return this.post(`/api/family/authorization/requests/${requestId}/reject`);
    }

    // ── Phase 4: AI 健康预测 & 统计分析 ──
    async getStatisticsOverview() {
        return this.get('/api/admin/statistics/overview');
    }

    async getRegionalStats(institutionId = null) {
        const path = institutionId ? `/api/admin/statistics/regional?institution_id=${institutionId}` : '/api/admin/statistics/regional';
        return this.get(path);
    }

    async getElderlyTrends(elderlyId, metric = 'alarms', days = 30) {
        return this.get(`/api/admin/statistics/elderly/${elderlyId}?metric=${metric}&days=${days}`);
    }

    async getAlarmTrends(days = 30) {
        return this.get(`/api/admin/statistics/alarms?days=${days}`);
    }

    async getFallRisk(elderlyId) {
        return this.post('/api/admin/ai/fall-risk', { elderly_id: elderlyId });
    }

    async getHealthTrend(elderlyId, days = 30) {
        return this.post('/api/admin/ai/health-trend', { elderly_id: elderlyId, days });
    }

    async getHealthReport(elderlyId) {
        return this.get(`/api/admin/ai/health-report/${elderlyId}`);
    }

    async getSleepPattern(elderlyId, days = 7) {
        return this.post('/api/admin/ai/behavior/sleep', { elderly_id: elderlyId, days });
    }

    async getActivityLevel(elderlyId, days = 7) {
        return this.post('/api/admin/ai/behavior/activity', { elderly_id: elderlyId, days });
    }

    async getAnomalyDetect(elderlyId) {
        return this.get(`/api/admin/ai/behavior/anomaly/${elderlyId}`);
    }
}

// 全局 API 实例
const api = new ApiClient();
