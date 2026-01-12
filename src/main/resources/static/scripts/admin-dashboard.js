// Admin Dashboard JavaScript
// Handles dashboard statistics and charts

let revenueChart = null;
let userGrowthChart = null;
let creditUsageChart = null;

document.addEventListener('DOMContentLoaded', () => {
    initSidebar();
    checkAuth();
    loadDashboardStats();
    loadCharts();
    loadCreditAnalytics();
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

    // Close sidebar when clicking outside on mobile
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
    if (!AUTH.isLoggedIn()) {
        window.location.href = '/html/login.html';
        return;
    }

    try {
        // Load admin user info and check role
        const user = await window.apiClient.get('/api/auth/me');
        
        console.log('User info:', user); // Debug log
        
        // Check if user has ADMIN role (handle both 'ADMIN' and 'ROLE_ADMIN')
        const isAdmin = user.role === 'ADMIN' || user.role === 'ROLE_ADMIN' || 
                       (user.roles && (user.roles.includes('ADMIN') || user.roles.includes('ROLE_ADMIN')));
        
        if (!isAdmin) {
            console.error('Access denied: User is not admin. Role:', user.role);
            showAdminToast('Bạn không có quyền truy cập trang này', 'error');
            setTimeout(() => {
                window.location.href = '/index.html';
            }, 2000);
            return;
        }
        
        // User is admin, show name
        document.getElementById('adminUserName').textContent = user.fullName || user.email;
        
    } catch (err) {
        console.error('Failed to load user info:', err);
        showAdminToast('Không thể tải thông tin user', 'error');
        setTimeout(() => {
            window.location.href = '/html/login.html';
        }, 2000);
    }
}

function logout() {
    AUTH.clearAuth();
    window.location.href = '/html/login.html';
}

// ==================== LOAD STATISTICS ====================

async function loadDashboardStats() {
    try {
        const stats = await window.apiClient.get('/api/admin/stats');

        // User statistics
        document.getElementById('totalUsers').textContent = formatNumber(stats.totalUsers);
        document.getElementById('activeUsers').textContent = formatNumber(stats.activeUsers);
        document.getElementById('newUsers').textContent = formatNumber(stats.newUsersLast30Days);
        document.getElementById('bannedUsers').textContent = formatNumber(stats.bannedUsers);

        // Revenue statistics
        document.getElementById('totalRevenue').textContent = formatCurrency(stats.totalRevenue);
        document.getElementById('revenue30Days').textContent = formatCurrency(stats.revenueLast30Days);
        document.getElementById('totalPayments').textContent = formatNumber(stats.totalSuccessfulPayments);

        // Activity statistics
        document.getElementById('totalQuizSets').textContent = formatNumber(stats.totalQuizSets);
        document.getElementById('totalQuizAttempts').textContent = formatNumber(stats.totalQuizAttempts);
        document.getElementById('totalChatSessions').textContent = formatNumber(stats.totalChatSessions);
        document.getElementById('totalChatMessages').textContent = formatNumber(stats.totalChatMessages);

    } catch (error) {
        console.error('Failed to load dashboard stats:', error);
        showAdminToast('Không thể tải thống kê dashboard', 'error');
    }
}

// ==================== LOAD CHARTS ====================

async function loadCharts() {
    try {
        // Calculate date range (last 30 days)
        const to = new Date();
        const from = new Date();
        from.setDate(from.getDate() - 30);

        const fromStr = from.toISOString().split('T')[0];
        const toStr = to.toISOString().split('T')[0];

        // Load revenue chart
        const revenueData = await window.apiClient.get(`/api/admin/stats/revenue?from=${fromStr}&to=${toStr}`);
        renderRevenueChart(revenueData);

        // Load user growth chart
        const userGrowthData = await window.apiClient.get(`/api/admin/stats/user-growth?from=${fromStr}&to=${toStr}`);
        renderUserGrowthChart(userGrowthData);

    } catch (error) {
        console.error('Failed to load charts:', error);
        showAdminToast('Không thể tải biểu đồ', 'error');
    }
}

function renderRevenueChart(data) {
    const ctx = document.getElementById('revenueChart');
    if (!ctx) return;

    // Destroy existing chart
    if (revenueChart) {
        revenueChart.destroy();
    }

    const labels = data.map(item => formatDate(item.date));
    const revenues = data.map(item => item.revenue);

    revenueChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Doanh thu (VNĐ)',
                data: revenues,
                borderColor: '#1a4b84',
                backgroundColor: 'rgba(26, 75, 132, 0.1)',
                borderWidth: 3,
                fill: true,
                tension: 0.4,
                pointRadius: 4,
                pointHoverRadius: 6,
                pointBackgroundColor: '#1a4b84',
                pointBorderColor: '#fff',
                pointBorderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        font: {
                            family: 'Inter',
                            weight: 600
                        }
                    }
                },
                tooltip: {
                    backgroundColor: '#0f172a',
                    titleFont: {
                        family: 'Inter',
                        weight: 600
                    },
                    bodyFont: {
                        family: 'Inter'
                    },
                    callbacks: {
                        label: function(context) {
                            return 'Doanh thu: ' + formatCurrency(context.parsed.y);
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    },
                    ticks: {
                        font: {
                            family: 'Inter'
                        },
                        callback: function(value) {
                            return formatCurrency(value);
                        }
                    }
                },
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        font: {
                            family: 'Inter'
                        }
                    }
                }
            }
        }
    });
}

function renderUserGrowthChart(data) {
    const ctx = document.getElementById('userGrowthChart');
    if (!ctx) return;

    // Destroy existing chart
    if (userGrowthChart) {
        userGrowthChart.destroy();
    }

    const labels = data.map(item => formatDate(item.date));
    const newUsers = data.map(item => item.newUsers);
    const totalUsers = data.map(item => item.totalUsers);

    userGrowthChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Users mới',
                    data: newUsers,
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    pointBackgroundColor: '#10b981',
                    pointBorderColor: '#fff',
                    pointBorderWidth: 2
                },
                {
                    label: 'Tổng users',
                    data: totalUsers,
                    borderColor: '#1a4b84',
                    backgroundColor: 'rgba(26, 75, 132, 0.1)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    pointBackgroundColor: '#1a4b84',
                    pointBorderColor: '#fff',
                    pointBorderWidth: 2
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        font: {
                            family: 'Inter',
                            weight: 600
                        }
                    }
                },
                tooltip: {
                    backgroundColor: '#0f172a',
                    titleFont: {
                        family: 'Inter',
                        weight: 600
                    },
                    bodyFont: {
                        family: 'Inter'
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    },
                    ticks: {
                        stepSize: 1,
                        font: {
                            family: 'Inter'
                        }
                    }
                },
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        font: {
                            family: 'Inter'
                        }
                    }
                }
            }
        }
    });
}

// ==================== UTILITY FUNCTIONS ====================

function formatNumber(num) {
    if (num === null || num === undefined) return '0';
    return num.toLocaleString('vi-VN');
}

function formatCurrency(amount) {
    if (amount === null || amount === undefined) return '0 ₫';
    return amount.toLocaleString('vi-VN') + ' ₫';
}

function formatDate(dateStr) {
    const date = new Date(dateStr);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    return `${day}/${month}`;
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


// ==================== CREDIT ANALYTICS ====================

async function loadCreditAnalytics() {
    try {
        // Calculate date range (last 30 days)
        const to = new Date();
        const from = new Date();
        from.setDate(from.getDate() - 30);

        const fromStr = from.toISOString().split('T')[0];
        const toStr = to.toISOString().split('T')[0];

        const analytics = await window.apiClient.get(`/api/admin/credits/analytics?from=${fromStr}&to=${toStr}`);

        // Update stats
        document.getElementById('totalChatUsed').textContent = formatNumber(analytics.totalChatUsed);
        document.getElementById('totalQuizGenUsed').textContent = formatNumber(analytics.totalQuizGenUsed);
        document.getElementById('totalChatPurchased').textContent = formatNumber(analytics.totalChatPurchased);
        document.getElementById('totalRefunded').textContent = formatNumber(analytics.totalChatRefunded + analytics.totalQuizGenRefunded);

        // Render chart
        renderCreditUsageChart(analytics.usageByDay);

    } catch (error) {
        console.error('Failed to load credit analytics:', error);
        // Don't show error toast - this is optional feature
    }
}

function renderCreditUsageChart(data) {
    const ctx = document.getElementById('creditUsageChart');
    if (!ctx) return;

    // Destroy existing chart
    if (creditUsageChart) {
        creditUsageChart.destroy();
    }

    const labels = data.map(item => formatDate(item.date));
    const chatUsed = data.map(item => item.chatUsed);
    const quizGenUsed = data.map(item => item.quizGenUsed);

    creditUsageChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Chat Credits',
                    data: chatUsed,
                    backgroundColor: 'rgba(26, 75, 132, 0.8)',
                    borderColor: '#1a4b84',
                    borderWidth: 1,
                    borderRadius: 4
                },
                {
                    label: 'Quiz Gen Credits',
                    data: quizGenUsed,
                    backgroundColor: 'rgba(245, 158, 11, 0.8)',
                    borderColor: '#f59e0b',
                    borderWidth: 1,
                    borderRadius: 4
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        font: {
                            family: 'Inter',
                            weight: 600
                        }
                    }
                },
                tooltip: {
                    backgroundColor: '#0f172a',
                    titleFont: {
                        family: 'Inter',
                        weight: 600
                    },
                    bodyFont: {
                        family: 'Inter'
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    stacked: true,
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    },
                    ticks: {
                        stepSize: 1,
                        font: {
                            family: 'Inter'
                        }
                    }
                },
                x: {
                    stacked: true,
                    grid: {
                        display: false
                    },
                    ticks: {
                        font: {
                            family: 'Inter'
                        }
                    }
                }
            }
        }
    });
}
