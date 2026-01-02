// Admin Activity Logs JavaScript

let currentPage = 0;

document.addEventListener('DOMContentLoaded', () => {
    initSidebar();
    checkAuth();
    loadActivityLogs();
});

// ==================== SIDEBAR ====================

function initSidebar() {
    const sidebar = document.getElementById('adminSidebar');
    const toggleBtn = document.getElementById('toggleSidebar');
    const closeBtn = document.getElementById('closeSidebar');

    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.add('show');
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            sidebar.classList.remove('show');
        });
    }

    document.addEventListener('click', (e) => {
        if (window.innerWidth < 992) {
            if (!sidebar.contains(e.target) && !toggleBtn.contains(e.target)) {
                sidebar.classList.remove('show');
            }
        }
    });
}

// ==================== AUTH ====================

async function checkAuth() {
    const token = localStorage.getItem('accessToken');
    if (!token) {
        window.location.href = '/html/login.html';
        return;
    }

    try {
        const user = await window.apiClient.get('/api/auth/me');
        
        console.log('User info:', user); // Debug log
        
        // Check if user has ADMIN role (handle both 'ADMIN' and 'ROLE_ADMIN')
        const isAdmin = user.role === 'ADMIN' || user.role === 'ROLE_ADMIN' || 
                       (user.roles && (user.roles.includes('ADMIN') || user.roles.includes('ROLE_ADMIN')));
        
        if (!isAdmin) {
            console.error('Access denied: User is not admin. Role:', user.role);
            alert('Bạn không có quyền truy cập trang này');
            window.location.href = '/index.html';
            return;
        }
        
        document.getElementById('adminUserName').textContent = user.fullName || user.email;
    } catch (err) {
        console.error('Failed to load user info:', err);
        window.location.href = '/html/login.html';
    }
}

function logout() {
    localStorage.removeItem('accessToken'); // Changed from 'token' to 'accessToken'
    localStorage.removeItem('refreshToken');
    window.location.href = '/html/login.html';
}

// ==================== LOAD ACTIVITY LOGS ====================

async function loadActivityLogs(page = 0) {
    currentPage = page;
    
    try {
        const url = `/api/admin/activity-logs?page=${page}&size=50`;
        const response = await window.apiClient.get(url);
        
        renderLogsTable(response.logs);
        renderPagination(response);
        
        document.getElementById('totalLogsCount').textContent = `${response.totalItems} logs`;
        
    } catch (error) {
        console.error('Failed to load activity logs:', error);
        showAdminToast('Không thể tải activity logs', 'error');
        
        document.getElementById('logsTableBody').innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-danger">
                    Không thể tải activity logs. Vui lòng thử lại.
                </td>
            </tr>
        `;
    }
}

// ==================== RENDER TABLE ====================

function renderLogsTable(logs) {
    const tbody = document.getElementById('logsTableBody');
    
    if (!logs || logs.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="text-center text-muted py-4">
                    <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                    Không có activity log nào
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = logs.map(log => `
        <tr>
            <td>
                <div class="user-info-cell">
                    <div class="user-avatar-sm">
                        ${log.adminUserName ? log.adminUserName.charAt(0).toUpperCase() : 'A'}
                    </div>
                    <div class="user-details">
                        <div class="user-name">${escapeHtml(log.adminUserName || 'Unknown')}</div>
                        <div class="user-email">${escapeHtml(log.adminUserEmail || '')}</div>
                    </div>
                </div>
            </td>
            <td>${renderActionBadge(log.actionType)}</td>
            <td>
                <div class="log-description">
                    <small>${escapeHtml(log.description)}</small>
                    <div class="text-muted" style="font-size: 0.7rem;">
                        ${escapeHtml(log.targetType)} #${log.targetId}
                    </div>
                </div>
            </td>
            <td>
                <div class="datetime-cell">
                    <div class="date">${formatDate(log.createdAt)}</div>
                    <div class="time">${formatTime(log.createdAt)}</div>
                </div>
            </td>
        </tr>
    `).join('');
}

function renderActionBadge(actionType) {
    const badges = {
        'BAN_USER': '<span class="badge bg-danger">Ban User</span>',
        'UNBAN_USER': '<span class="badge bg-success">Unban User</span>',
        'DELETE_USER': '<span class="badge bg-danger">Delete User</span>',
        'UPDATE_USER': '<span class="badge bg-warning">Update User</span>',
        'CREATE_ADMIN': '<span class="badge bg-primary">Create Admin</span>',
        'DELETE_PAYMENT': '<span class="badge bg-danger">Delete Payment</span>',
        'REFUND_PAYMENT': '<span class="badge bg-warning">Refund Payment</span>'
    };
    return badges[actionType] || `<span class="badge bg-info">${escapeHtml(actionType)}</span>`;
}

// ==================== PAGINATION ====================

function renderPagination(response) {
    const pagination = document.getElementById('pagination');
    const { currentPage, totalPages, hasPrevious, hasNext } = response;
    
    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }
    
    let html = '';
    
    // Previous button
    html += `
        <li class="page-item ${!hasPrevious ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadActivityLogs(${currentPage - 1}); return false;">
                <i class="bi bi-chevron-left"></i>
            </a>
        </li>
    `;
    
    // Page numbers
    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(totalPages - 1, currentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        html += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" onclick="loadActivityLogs(${i}); return false;">${i + 1}</a>
            </li>
        `;
    }
    
    // Next button
    html += `
        <li class="page-item ${!hasNext ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadActivityLogs(${currentPage + 1}); return false;">
                <i class="bi bi-chevron-right"></i>
            </a>
        </li>
    `;
    
    pagination.innerHTML = html;
}

// ==================== UTILITY FUNCTIONS ====================

function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString('vi-VN');
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('vi-VN');
}

function formatTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showAdminToast(message, type = 'info') {
    // Use the global toast notification if available
    if (typeof window.showToast === 'function') {
        window.showToast(message, type);
    } else {
        // Fallback to alert
        alert(message);
    }
}
