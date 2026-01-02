// Quiz History Page Script - Paginated Version
const API_BASE = '/api/quiz-sets';
const PAGE_SIZE = 10;

let currentPage = 0;
let paginationData = null;
let scoreChart = null;

document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
        window.location.href = '/html/login.html';
        return;
    }

    document.getElementById('btnNewTest')?.addEventListener('click', () => {
        window.location.href = '/html/my-quizzes.html';
    });

    document.getElementById('btnStartTest')?.addEventListener('click', () => {
        window.location.href = '/html/my-quizzes.html';
    });

    document.getElementById('btnThisMonth')?.addEventListener('click', () => {
        currentPage = 0;
        loadHistory();
    });

    document.getElementById('btnViewAll')?.addEventListener('click', () => {
        currentPage = 0;
        loadHistory();
    });

    loadHistory();
});

async function loadHistory() {
    ERROR_HANDLER.showLoading(true);
    try {
        console.log('Loading history page:', currentPage);
        paginationData = await API_CLIENT.get(
            API_BASE + '/exam/history?page=' + currentPage + '&size=' + PAGE_SIZE
        );
        console.log('History data received:', paginationData);
        renderStats();
        renderChart();
        renderHistory();
        renderPagination();
    } catch (err) {
        console.error('Error loading history:', err);
        Toast.show('Có lỗi xảy ra khi tải lịch sử', 'error', 3000);
    } finally {
        ERROR_HANDLER.showLoading(false);
    }
}


function renderStats() {
    const totalTests = paginationData?.totalElements || 0;
    const currentAttempts = paginationData?.content || [];
    const avgAccuracy = currentAttempts.length > 0
        ? Math.round(currentAttempts.reduce((sum, a) => sum + a.scorePercent, 0) / currentAttempts.length)
        : 0;

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const testsToday = currentAttempts.filter(a => {
        const attemptDate = new Date(a.finishedAt);
        attemptDate.setHours(0, 0, 0, 0);
        return attemptDate.getTime() === today.getTime();
    }).length;

    document.getElementById('statTotalTests').textContent = totalTests;
    document.getElementById('statTestsTrend').textContent = '+' + testsToday + ' hôm nay';
    document.getElementById('statAvgAccuracy').textContent = avgAccuracy + '%';
    document.getElementById('statAIQueries').textContent = totalTests * 3;
    document.getElementById('statQueriesTrend').textContent = '+' + (testsToday * 3) + ' tuần này';
}

function renderChart() {
    const ctx = document.getElementById('scoreChart');
    if (!ctx) return;

    const currentAttempts = paginationData?.content || [];
    const last7Days = [];
    const today = new Date();
    
    for (let i = 6; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        date.setHours(0, 0, 0, 0);
        last7Days.push(date);
    }

    const chartData = last7Days.map(date => {
        const dayAttempts = currentAttempts.filter(a => {
            const attemptDate = new Date(a.finishedAt);
            attemptDate.setHours(0, 0, 0, 0);
            return attemptDate.getTime() === date.getTime();
        });
        if (dayAttempts.length === 0) return null;
        const avgScore = dayAttempts.reduce((sum, a) => sum + (a.correctCount / a.totalQuestions * 10), 0) / dayAttempts.length;
        return avgScore.toFixed(1);
    });

    const labels = last7Days.map(date => {
        const days = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
        return days[date.getDay()];
    });

    if (scoreChart) scoreChart.destroy();

    scoreChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Điểm trung bình',
                data: chartData,
                borderColor: '#1a4b84',
                backgroundColor: 'rgba(26, 75, 132, 0.1)',
                borderWidth: 3,
                tension: 0.4,
                fill: true,
                pointRadius: 5,
                pointHoverRadius: 7,
                pointBackgroundColor: '#1a4b84',
                pointBorderColor: '#fff',
                pointBorderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#ffffff',
                    titleColor: '#1e293b',
                    bodyColor: '#1e293b',
                    borderColor: '#e2e8f0',
                    borderWidth: 1,
                    padding: 12,
                    displayColors: false
                }
            },
            scales: {
                y: { beginAtZero: true, max: 10, ticks: { color: '#64748b', stepSize: 2 }, grid: { color: '#e2e8f0' } },
                x: { ticks: { color: '#64748b' }, grid: { display: false } }
            }
        }
    });
}


function renderHistory() {
    const historyList = document.getElementById('historyList');
    const emptyState = document.getElementById('emptyState');
    const attempts = paginationData?.content || [];

    if (!attempts || attempts.length === 0) {
        historyList.innerHTML = '';
        emptyState.classList.remove('d-none');
        return;
    }

    emptyState.classList.add('d-none');
    historyList.innerHTML = attempts.map(attempt => {
        const score = (attempt.correctCount / attempt.totalQuestions * 10).toFixed(1);
        const scoreNum = parseFloat(score);
        
        let badgeClass = 'badge-poor';
        let badgeText = 'Còn cố gắng';
        if (scoreNum >= 8) { badgeClass = 'badge-excellent'; badgeText = 'Xuất sắc'; }
        else if (scoreNum >= 6.5) { badgeClass = 'badge-good'; badgeText = 'Khá'; }
        else if (scoreNum >= 5) { badgeClass = 'badge-average'; badgeText = 'Trung bình'; }

        const timeAgo = getTimeAgo(new Date(attempt.finishedAt));
        const title = escapeHtml(attempt.quizSetTitle || 'Bộ đề');

        return '<div class="history-item" onclick="viewAttemptDetail(' + attempt.quizSetId + ')">' +
            '<div class="history-item-icon"><i class="bi bi-file-earmark-text"></i></div>' +
            '<div class="history-item-content">' +
                '<div class="history-item-title">' + title + '</div>' +
                '<div class="history-item-meta">' +
                    '<span><i class="bi bi-clock"></i> ' + timeAgo + '</span>' +
                    '<span><i class="bi bi-question-circle"></i> ' + attempt.totalQuestions + ' câu</span>' +
                    '<span><i class="bi bi-check-circle"></i> ' + attempt.correctCount + ' đúng</span>' +
                '</div>' +
            '</div>' +
            '<div class="history-item-score">' +
                '<div class="history-score-value">' + score + '<span style="font-size:1rem;color:#9ca3af">/10</span></div>' +
                '<div class="history-score-badge ' + badgeClass + '">' + badgeText + '</div>' +
            '</div>' +
        '</div>';
    }).join('');
}

function renderPagination() {
    let paginationNav = document.getElementById('paginationNav');
    if (!paginationNav) {
        paginationNav = document.createElement('nav');
        paginationNav.id = 'paginationNav';
        paginationNav.className = 'mt-4';
        paginationNav.innerHTML = '<ul class="pagination justify-content-center" id="paginationControls"></ul>';
        document.querySelector('.history-list-section')?.appendChild(paginationNav);
    }

    if (!paginationData || paginationData.totalPages <= 1) {
        paginationNav.classList.add('d-none');
        return;
    }

    paginationNav.classList.remove('d-none');
    const controls = document.getElementById('paginationControls');
    controls.innerHTML = '';

    // Previous
    const prevLi = document.createElement('li');
    prevLi.className = 'page-item' + (paginationData.first ? ' disabled' : '');
    prevLi.innerHTML = '<a class="page-link" href="#"><i class="bi bi-chevron-left"></i></a>';
    if (!paginationData.first) {
        prevLi.querySelector('a').onclick = (e) => { e.preventDefault(); goToPage(currentPage - 1); };
    }
    controls.appendChild(prevLi);

    // Pages
    const totalPages = paginationData.totalPages;
    for (let i = 0; i < totalPages && i < 5; i++) {
        const li = document.createElement('li');
        li.className = 'page-item' + (i === currentPage ? ' active' : '');
        li.innerHTML = '<a class="page-link" href="#">' + (i + 1) + '</a>';
        if (i !== currentPage) {
            const pageNum = i;
            li.querySelector('a').onclick = (e) => { e.preventDefault(); goToPage(pageNum); };
        }
        controls.appendChild(li);
    }

    // Next
    const nextLi = document.createElement('li');
    nextLi.className = 'page-item' + (paginationData.last ? ' disabled' : '');
    nextLi.innerHTML = '<a class="page-link" href="#"><i class="bi bi-chevron-right"></i></a>';
    if (!paginationData.last) {
        nextLi.querySelector('a').onclick = (e) => { e.preventDefault(); goToPage(currentPage + 1); };
    }
    controls.appendChild(nextLi);
}

function goToPage(page) {
    currentPage = page;
    loadHistory();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function viewAttemptDetail(quizSetId) {
    window.location.href = '/html/quiz-take.html?setId=' + quizSetId;
}

function getTimeAgo(date) {
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Vừa xong';
    if (diffMins < 60) return diffMins + ' phút trước';
    if (diffHours < 24) return diffHours + ' giờ trước';
    if (diffDays < 7) return diffDays + ' ngày trước';
    return date.toLocaleDateString('vi-VN');
}

function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
