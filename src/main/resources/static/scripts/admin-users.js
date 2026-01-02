// Admin Users Management JavaScript

let currentPage = 0;
let currentSearch = '';
let currentStatus = '';
let selectedUserId = null;

document.addEventListener('DOMContentLoaded', () => {
    initSidebar();
    checkAuth();
    loadUsers();
    
    // Search on Enter key
    document.getElementById('searchInput').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            searchUsers();
        }
    });
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

// ==================== LOAD USERS ====================

async function loadUsers(page = 0) {
    currentPage = page;
    
    try {
        let url = `/api/admin/users?page=${page}&size=20&sort=createdAt&direction=DESC`;
        
        if (currentSearch) {
            url += `&search=${encodeURIComponent(currentSearch)}`;
        }
        
        const response = await window.apiClient.get(url);
        
        renderUsersTable(response.users);
        renderPagination(response);
        
        document.getElementById('totalUsersCount').textContent = `${response.totalItems} users`;
        
    } catch (error) {
        console.error('Failed to load users:', error);
        showAdminToast('Không thể tải danh sách users', 'error');
        
        document.getElementById('usersTableBody').innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-danger">
                    Không thể tải danh sách users. Vui lòng thử lại.
                </td>
            </tr>
        `;
    }
}

function searchUsers() {
    currentSearch = document.getElementById('searchInput').value.trim();
    currentStatus = document.getElementById('statusFilter').value;
    loadUsers(0);
}

// ==================== RENDER TABLE ====================

function renderUsersTable(users) {
    const tbody = document.getElementById('usersTableBody');
    
    if (!users || users.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-muted py-4">
                    <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                    Không tìm thấy user nào
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = users.map(user => `
        <tr>
            <td>
                <div class="user-info-cell">
                    <div class="user-avatar-sm">
                        ${user.fullName ? user.fullName.charAt(0).toUpperCase() : user.email.charAt(0).toUpperCase()}
                    </div>
                    <div class="user-details">
                        <div class="user-name" title="${escapeHtml(user.fullName || user.email)}">${escapeHtml(user.fullName || '-')}</div>
                        <div class="user-email" title="${escapeHtml(user.email)}">${escapeHtml(user.email)}</div>
                    </div>
                </div>
            </td>
            <td><span class="badge bg-secondary">${user.provider}</span></td>
            <td>
                <div class="credits-display">
                    <span><i class="bi bi-chat-dots text-primary"></i> ${user.chatCredits || 0}</span>
                    <span><i class="bi bi-lightbulb text-warning"></i> ${user.quizGenCredits || 0}</span>
                </div>
            </td>
            <td>${renderStatusBadge(user)}</td>
            <td>
                <div class="datetime-cell">
                    <div class="date">${formatDate(user.createdAt)}</div>
                    <div class="time">${formatTime(user.createdAt)}</div>
                </div>
            </td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-info btn-action" onclick="viewUserDetail(${user.id})" title="Xem chi tiết">
                        <i class="bi bi-eye"></i>
                    </button>
                    ${user.active ? `
                        <button class="btn btn-warning btn-action" onclick="showBanModal(${user.id}, '${escapeHtml(user.email)}', '${escapeHtml(user.fullName || '')}')" title="Ban">
                            <i class="bi bi-ban"></i>
                        </button>
                    ` : `
                        <button class="btn btn-success btn-action" onclick="unbanUser(${user.id})" title="Unban">
                            <i class="bi bi-check-circle"></i>
                        </button>
                    `}
                </div>
            </td>
        </tr>
    `).join('');
}

function renderStatusBadge(user) {
    if (!user.enabled) {
        return '<span class="badge bg-secondary">Disabled</span>';
    }
    if (!user.active) {
        return '<span class="badge bg-danger">Banned</span>';
    }
    if (!user.emailVerified) {
        return '<span class="badge bg-warning">Chưa verify</span>';
    }
    return '<span class="badge bg-success">Active</span>';
}

// ==================== PAGINATION ====================

function renderPagination(response) {
    const pagination = document.getElementById('pagination');
    const { currentPage, totalPages, hasPrevious, hasNext } = response;
    
    let html = '';
    
    // Previous button
    html += `
        <li class="page-item ${!hasPrevious ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadUsers(${currentPage - 1}); return false;">
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
                <a class="page-link" href="#" onclick="loadUsers(${i}); return false;">${i + 1}</a>
            </li>
        `;
    }
    
    // Next button
    html += `
        <li class="page-item ${!hasNext ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadUsers(${currentPage + 1}); return false;">
                <i class="bi bi-chevron-right"></i>
            </a>
        </li>
    `;
    
    pagination.innerHTML = html;
}

// ==================== USER DETAIL ====================

async function viewUserDetail(userId) {
    try {
        const user = await window.apiClient.get(`/api/admin/users/${userId}`);
        
        const avatarInitial = user.fullName ? user.fullName.charAt(0).toUpperCase() : user.email.charAt(0).toUpperCase();
        const avatarContent = user.avatarUrl 
            ? `<img src="${escapeHtml(user.avatarUrl)}" alt="Avatar">`
            : avatarInitial;
        
        const content = `
            <!-- User Header -->
            <div class="user-detail-header">
                <div class="user-detail-avatar">${avatarContent}</div>
                <div class="user-detail-info">
                    <h4>${escapeHtml(user.fullName || 'Chưa cập nhật')}</h4>
                    <div class="user-email">${escapeHtml(user.email)}</div>
                    <div class="user-badges">
                        <span class="badge bg-secondary">${user.provider}</span>
                        ${user.emailVerified ? '<span class="badge bg-info">Email Verified</span>' : '<span class="badge bg-warning">Chưa verify email</span>'}
                        ${user.active ? '<span class="badge bg-success">Active</span>' : '<span class="badge bg-danger">Banned</span>'}
                    </div>
                </div>
            </div>
            
            ${!user.active ? `
                <div class="ban-alert">
                    <div class="ban-alert-title">
                        <i class="bi bi-ban"></i>
                        Tài khoản đã bị khóa
                    </div>
                    <div class="ban-alert-reason">${escapeHtml(user.banReason || 'Không có lý do')}</div>
                    <div class="ban-alert-meta">
                        <i class="bi bi-person"></i> ${user.bannedByUserName || 'Unknown'} · 
                        <i class="bi bi-clock"></i> ${formatDateTime(user.bannedAt)}
                    </div>
                </div>
            ` : ''}
            
            <div class="row g-3">
                <!-- Basic Info -->
                <div class="col-md-6">
                    <div class="user-detail-section">
                        <div class="user-detail-section-title">
                            <i class="bi bi-person-badge"></i>
                            Thông Tin Cơ Bản
                        </div>
                        <div class="user-detail-grid">
                            <div class="user-detail-item">
                                <span class="label">ID</span>
                                <span class="value">#${user.id}</span>
                            </div>
                            <div class="user-detail-item">
                                <span class="label">Provider</span>
                                <span class="value">${user.provider}</span>
                            </div>
                            <div class="user-detail-item">
                                <span class="label">Trạng thái</span>
                                <span class="value ${user.enabled ? 'success' : 'danger'}">${user.enabled ? 'Enabled' : 'Disabled'}</span>
                            </div>
                            <div class="user-detail-item">
                                <span class="label">Ngày tạo</span>
                                <span class="value">${formatDateTime(user.createdAt)}</span>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Credits -->
                <div class="col-md-6">
                    <div class="user-detail-section">
                        <div class="user-detail-section-title">
                            <i class="bi bi-coin"></i>
                            Credits
                        </div>
                        <div class="user-detail-grid">
                            <div class="user-detail-item">
                                <span class="label">Chat Credits</span>
                                <span class="value highlight">${user.chatCredits || 0}</span>
                            </div>
                            <div class="user-detail-item">
                                <span class="label">Quiz Gen Credits</span>
                                <span class="value highlight">${user.quizGenCredits || 0}</span>
                            </div>
                            <div class="user-detail-item" style="grid-column: span 2;">
                                <span class="label">Hết hạn</span>
                                <span class="value">${user.creditsExpiresAt ? formatDateTime(user.creditsExpiresAt) : 'Không giới hạn'}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Statistics -->
            <div class="user-detail-section">
                <div class="user-detail-section-title">
                    <i class="bi bi-graph-up"></i>
                    Thống Kê Hoạt Động
                </div>
                <div class="user-stats-grid">
                    <div class="user-stat-card">
                        <div class="stat-icon success">
                            <i class="bi bi-credit-card"></i>
                        </div>
                        <div class="stat-value">${user.totalPayments || 0}</div>
                        <div class="stat-label">Thanh toán</div>
                    </div>
                    <div class="user-stat-card">
                        <div class="stat-icon primary">
                            <i class="bi bi-cash-stack"></i>
                        </div>
                        <div class="stat-value">${formatCurrencyShort(user.totalRevenue || 0)}</div>
                        <div class="stat-label">Doanh thu</div>
                    </div>
                    <div class="user-stat-card">
                        <div class="stat-icon warning">
                            <i class="bi bi-collection"></i>
                        </div>
                        <div class="stat-value">${user.totalQuizSets || 0}</div>
                        <div class="stat-label">Bộ đề</div>
                    </div>
                    <div class="user-stat-card">
                        <div class="stat-icon info">
                            <i class="bi bi-pencil-square"></i>
                        </div>
                        <div class="stat-value">${user.totalQuizAttempts || 0}</div>
                        <div class="stat-label">Lần thi</div>
                    </div>
                    <div class="user-stat-card">
                        <div class="stat-icon primary">
                            <i class="bi bi-chat-dots"></i>
                        </div>
                        <div class="stat-value">${user.totalChatSessions || 0}</div>
                        <div class="stat-label">Phiên chat</div>
                    </div>
                    <div class="user-stat-card">
                        <div class="stat-icon success">
                            <i class="bi bi-chat-text"></i>
                        </div>
                        <div class="stat-value">${user.totalChatMessages || 0}</div>
                        <div class="stat-label">Tin nhắn</div>
                    </div>
                </div>
            </div>
        `;
        
        document.getElementById('userDetailContent').innerHTML = content;
        
        // Add class to modal for custom styling
        document.getElementById('userDetailModal').classList.add('user-detail-modal');
        
        const modal = new bootstrap.Modal(document.getElementById('userDetailModal'));
        modal.show();
        
    } catch (error) {
        console.error('Failed to load user detail:', error);
        showAdminToast('Không thể tải chi tiết user', 'error');
    }
}

// ==================== BAN USER ====================

function showBanModal(userId, email, fullName = null) {
    selectedUserId = userId;
    document.getElementById('banUserEmail').textContent = email;
    document.getElementById('banReason').value = '';
    
    // Set avatar initial
    const avatarInitial = fullName ? fullName.charAt(0).toUpperCase() : email.charAt(0).toUpperCase();
    document.getElementById('banUserAvatar').textContent = avatarInitial;
    
    const modal = new bootstrap.Modal(document.getElementById('banUserModal'));
    modal.show();
}

async function confirmBanUser() {
    const reason = document.getElementById('banReason').value.trim();
    
    if (!reason) {
        showAdminToast('Vui lòng nhập lý do ban user', 'warning');
        return;
    }
    
    if (reason.length < 3) {
        showAdminToast('Lý do ban phải có ít nhất 3 ký tự', 'warning');
        return;
    }
    
    try {
        await window.apiClient.post(`/api/admin/users/${selectedUserId}/ban`, { reason });
        
        showAdminToast('Ban user thành công', 'success');
        
        const modal = bootstrap.Modal.getInstance(document.getElementById('banUserModal'));
        modal.hide();
        
        loadUsers(currentPage);
        
    } catch (error) {
        console.error('Failed to ban user:', error);
        const errorMsg = error.error || error.message || 'Unknown error';
        showAdminToast('Không thể ban user: ' + errorMsg, 'error');
    }
}

// ==================== UNBAN USER ====================

async function unbanUser(userId) {
    const confirmed = await window.ConfirmModal.show({
        title: 'Xác nhận Unban',
        message: 'Bạn có chắc muốn unban user này?',
        confirmText: 'Unban',
        cancelText: 'Hủy',
        type: 'info'
    });
    
    if (!confirmed) {
        return;
    }
    
    try {
        await window.apiClient.post(`/api/admin/users/${userId}/unban`, {});
        
        showAdminToast('Unban user thành công', 'success');
        loadUsers(currentPage);
        
    } catch (error) {
        console.error('Failed to unban user:', error);
        const errorMsg = error.error || error.message || 'Unknown error';
        showAdminToast('Không thể unban user: ' + errorMsg, 'error');
    }
}

// ==================== DELETE USER ====================

async function deleteUser(userId) {
    if (!confirm('Bạn có chắc muốn xóa user này? (Soft delete)')) {
        return;
    }
    
    try {
        await window.apiClient.delete(`/api/admin/users/${userId}`);
        
        showAdminToast('Xóa user thành công', 'success');
        loadUsers(currentPage);
        
    } catch (error) {
        console.error('Failed to delete user:', error);
        showAdminToast('Không thể xóa user: ' + (error.message || 'Unknown error'), 'error');
    }
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

function formatCurrency(amount) {
    if (amount === null || amount === undefined) return '0 ₫';
    return amount.toLocaleString('vi-VN') + ' ₫';
}

function formatCurrencyShort(amount) {
    if (amount === null || amount === undefined || amount === 0) return '0đ';
    if (amount >= 1000000) {
        return (amount / 1000000).toFixed(1).replace('.0', '') + 'M';
    }
    if (amount >= 1000) {
        return (amount / 1000).toFixed(0) + 'K';
    }
    return amount.toLocaleString('vi-VN') + 'đ';
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
