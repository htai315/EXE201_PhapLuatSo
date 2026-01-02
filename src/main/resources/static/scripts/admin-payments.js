// Admin Payments Management JavaScript

let currentPage = 0;

document.addEventListener('DOMContentLoaded', () => {
    initSidebar();
    checkAuth();
    loadPaymentStats();
    loadPayments();
});

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

async function checkAuth() {
    const token = localStorage.getItem('accessToken');
    if (!token) {
        window.location.href = '/html/login.html';
        return;
    }

    try {
        const user = await window.apiClient.get('/api/auth/me');
        const isAdmin = user.role === 'ADMIN' || user.role === 'ROLE_ADMIN' || 
                       (user.roles && (user.roles.includes('ADMIN') || user.roles.includes('ROLE_ADMIN')));
        
        if (!isAdmin) {
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
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/html/login.html';
}

async function loadPaymentStats() {
    try {
        const stats = await window.apiClient.get('/api/admin/payments/stats');

        document.getElementById('totalPayments').textContent = formatNumber(stats.totalPayments);
        document.getElementById('successfulPayments').textContent = formatNumber(stats.successfulPayments);
        document.getElementById('failedPayments').textContent = formatNumber(stats.failedPayments);
        document.getElementById('pendingPayments').textContent = formatNumber(stats.pendingPayments);

        document.getElementById('totalRevenue').textContent = formatCurrency(stats.totalRevenue);
        document.getElementById('revenueToday').textContent = formatCurrency(stats.revenueToday);
        document.getElementById('revenueThisWeek').textContent = formatCurrency(stats.revenueThisWeek);
        document.getElementById('revenueThisMonth').textContent = formatCurrency(stats.revenueThisMonth);

    } catch (error) {
        console.error('Failed to load payment stats:', error);
        showAdminToast('Không thể tải thống kê payments', 'error');
    }
}

async function loadPayments(page = 0) {
    currentPage = page;
    
    try {
        const url = `/api/admin/payments?page=${page}&size=20&sort=createdAt&direction=DESC`;
        const response = await window.apiClient.get(url);
        
        renderPaymentsTable(response.payments);
        renderPagination(response);
        
        document.getElementById('totalPaymentsCount').textContent = `${response.totalItems} giao dịch`;
        
    } catch (error) {
        console.error('Failed to load payments:', error);
        showAdminToast('Không thể tải danh sách payments', 'error');
        
        document.getElementById('paymentsTableBody').innerHTML = `
            <tr>
                <td colspan="5" class="text-center text-danger">
                    Không thể tải danh sách payments. Vui lòng thử lại.
                </td>
            </tr>
        `;
    }
}

function renderPaymentsTable(payments) {
    const tbody = document.getElementById('paymentsTableBody');
    
    if (!payments || payments.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center text-muted py-4">
                    <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                    Không tìm thấy payment nào
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = payments.map(payment => `
        <tr>
            <td>
                <div class="user-info-cell">
                    <div class="user-avatar-sm">
                        ${payment.userName ? payment.userName.charAt(0).toUpperCase() : payment.userEmail.charAt(0).toUpperCase()}
                    </div>
                    <div class="user-details">
                        <div class="user-name" title="${escapeHtml(payment.userName || payment.userEmail)}">${escapeHtml(payment.userName || '-')}</div>
                        <div class="user-email" title="${escapeHtml(payment.userEmail)}">${escapeHtml(payment.userEmail)}</div>
                    </div>
                </div>
            </td>
            <td><span class="badge bg-info">${escapeHtml(payment.planCode)}</span></td>
            <td><strong class="text-success">${formatCurrency(payment.amount)}</strong></td>
            <td>${renderStatusBadge(payment.status)}</td>
            <td>
                <div class="datetime-cell">
                    <div class="date">${formatDate(payment.createdAt)}</div>
                    <div class="time">${formatTime(payment.createdAt)}</div>
                </div>
            </td>
        </tr>
    `).join('');
}

function renderStatusBadge(status) {
    const badges = {
        'SUCCESS': '<span class="badge bg-success">Thành công</span>',
        'FAILED': '<span class="badge bg-danger">Thất bại</span>',
        'PENDING': '<span class="badge bg-warning">Đang chờ</span>',
        'CANCELLED': '<span class="badge bg-secondary">Đã hủy</span>'
    };
    return badges[status] || `<span class="badge bg-secondary">${status}</span>`;
}

function renderPagination(response) {
    const pagination = document.getElementById('pagination');
    const { currentPage, totalPages, hasPrevious, hasNext } = response;
    
    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }
    
    let html = '';
    
    html += `
        <li class="page-item ${!hasPrevious ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadPayments(${currentPage - 1}); return false;">
                <i class="bi bi-chevron-left"></i>
            </a>
        </li>
    `;
    
    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(totalPages - 1, currentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        html += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" onclick="loadPayments(${i}); return false;">${i + 1}</a>
            </li>
        `;
    }
    
    html += `
        <li class="page-item ${!hasNext ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadPayments(${currentPage + 1}); return false;">
                <i class="bi bi-chevron-right"></i>
            </a>
        </li>
    `;
    
    pagination.innerHTML = html;
}

function formatNumber(num) {
    if (num === null || num === undefined) return '0';
    return num.toLocaleString('vi-VN');
}

function formatCurrency(amount) {
    if (amount === null || amount === undefined) return '0 ₫';
    return amount.toLocaleString('vi-VN') + ' ₫';
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
    if (typeof window.showToast === 'function') {
        window.showToast(message, type);
    } else {
        alert(message);
    }
}
