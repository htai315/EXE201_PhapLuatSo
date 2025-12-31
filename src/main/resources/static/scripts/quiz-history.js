// Quiz History Page Script
const API_BASE = '/api/quiz-sets';
let allAttempts = [];
let scoreChart = null;

document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
        window.location.href = '/html/login.html';
        return;
    }

    // Event listeners
    document.getElementById('btnNewTest')?.addEventListener('click', () => {
        window.location.href = '/html/my-quizzes.html';
    });

    document.getElementById('btnStartTest')?.addEventListener('click', () => {
        window.location.href = '/html/my-quizzes.html';
    });

    document.getElementById('btnThisMonth')?.addEventListener('click', () => {
        filterByMonth();
    });

    document.getElementById('btnViewAll')?.addEventListener('click', () => {
        renderAllHistory();
    });

    loadAllHistory();
});

async function loadAllHistory() {
    ERROR_HANDLER.showLoading(true);
    try {
        // Get all quiz sets first
        const setsRes = await API_CLIENT.get(`${API_BASE}/my`);
        if (!setsRes.ok) {
            throw new Error('Không thể tải danh sách bộ đề');
        }
        const quizSets = await setsRes.json();

        // Get history for each quiz set
        allAttempts = [];
        for (const set of quizSets) {
            try {
                const historyRes = await API_CLIENT.get(`${API_BASE}/${set.id}/exam/history`);
                if (historyRes.ok) {
                    const historyData = await historyRes.json();
                    if (historyData.attempts && historyData.attempts.length > 0) {
                        historyData.attempts.forEach(attempt => {
                            allAttempts.push({
                                ...attempt,
                                quizSetTitle: historyData.quizSetTitle,
                                quizSetId: historyData.quizSetId
                            });
                        });
                    }
                }
            } catch (err) {
                console.error(`Error loading history for quiz set ${set.id}:`, err);
            }
        }

        // Sort by date (newest first)
        allAttempts.sort((a, b) => new Date(b.finishedAt) - new Date(a.finishedAt));

        renderStats();
        renderChart();
        renderHistory();
    } catch (err) {
        console.error(err);
        showError('Có lỗi xảy ra khi tải lịch sử');
    } finally {
        ERROR_HANDLER.showLoading(false);
    }
}

function renderStats() {
    const totalTests = allAttempts.length;
    const avgAccuracy = totalTests > 0
        ? Math.round(allAttempts.reduce((sum, a) => sum + a.scorePercent, 0) / totalTests)
        : 0;

    // Count tests today
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const testsToday = allAttempts.filter(a => {
        const attemptDate = new Date(a.finishedAt);
        attemptDate.setHours(0, 0, 0, 0);
        return attemptDate.getTime() === today.getTime();
    }).length;

    document.getElementById('statTotalTests').textContent = totalTests;
    document.getElementById('statTestsTrend').textContent = `+${testsToday} hôm nay`;
    document.getElementById('statAvgAccuracy').textContent = `${avgAccuracy}%`;
    
    // Mock AI queries stat (you can replace with real data)
    document.getElementById('statAIQueries').textContent = totalTests * 3;
    document.getElementById('statQueriesTrend').textContent = `+${testsToday * 3} tuần này`;
}

function renderChart() {
    const ctx = document.getElementById('scoreChart');
    if (!ctx) return;

    // Get last 7 days data
    const last7Days = [];
    const today = new Date();
    for (let i = 6; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        date.setHours(0, 0, 0, 0);
        last7Days.push(date);
    }

    // Calculate average score for each day
    const chartData = last7Days.map(date => {
        const dayAttempts = allAttempts.filter(a => {
            const attemptDate = new Date(a.finishedAt);
            attemptDate.setHours(0, 0, 0, 0);
            return attemptDate.getTime() === date.getTime();
        });

        if (dayAttempts.length === 0) return null;

        const avgScore = dayAttempts.reduce((sum, a) => {
            return sum + (a.correctCount / a.totalQuestions * 10);
        }, 0) / dayAttempts.length;

        return avgScore.toFixed(1);
    });

    const labels = last7Days.map(date => {
        const days = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
        return days[date.getDay()];
    });

    if (scoreChart) {
        scoreChart.destroy();
    }

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
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: '#ffffff',
                    titleColor: '#1e293b',
                    bodyColor: '#1e293b',
                    borderColor: '#e2e8f0',
                    borderWidth: 1,
                    padding: 12,
                    displayColors: false,
                    callbacks: {
                        label: function(context) {
                            return context.parsed.y !== null ? `Điểm: ${context.parsed.y}/10` : 'Chưa có dữ liệu';
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 10,
                    ticks: {
                        color: '#64748b',
                        stepSize: 2
                    },
                    grid: {
                        color: '#e2e8f0',
                        drawBorder: false
                    }
                },
                x: {
                    ticks: {
                        color: '#64748b'
                    },
                    grid: {
                        display: false
                    }
                }
            }
        }
    });
}

function renderHistory(limit = 10) {
    const historyList = document.getElementById('historyList');
    const emptyState = document.getElementById('emptyState');

    if (!allAttempts || allAttempts.length === 0) {
        historyList.innerHTML = '';
        emptyState.classList.remove('d-none');
        return;
    }

    emptyState.classList.add('d-none');
    const displayAttempts = limit ? allAttempts.slice(0, limit) : allAttempts;

    historyList.innerHTML = displayAttempts.map(attempt => {
        const score = (attempt.correctCount / attempt.totalQuestions * 10).toFixed(1);
        const scoreNum = parseFloat(score);
        
        let badgeClass = 'badge-poor';
        let badgeText = 'Còn cố gắng';
        if (scoreNum >= 8) {
            badgeClass = 'badge-excellent';
            badgeText = 'Xuất sắc';
        } else if (scoreNum >= 6.5) {
            badgeClass = 'badge-good';
            badgeText = 'Khá';
        } else if (scoreNum >= 5) {
            badgeClass = 'badge-average';
            badgeText = 'Trung bình';
        }

        const date = new Date(attempt.finishedAt);
        const timeAgo = getTimeAgo(date);

        return `
            <div class="history-item" onclick="viewAttemptDetail(${attempt.quizSetId}, ${attempt.attemptId})">
                <div class="history-item-icon">
                    <i class="bi bi-file-earmark-text"></i>
                </div>
                <div class="history-item-content">
                    <div class="history-item-title">${escapeHtml(attempt.quizSetTitle)}</div>
                    <div class="history-item-meta">
                        <span>
                            <i class="bi bi-clock"></i>
                            ${timeAgo}
                        </span>
                        <span>
                            <i class="bi bi-question-circle"></i>
                            ${attempt.totalQuestions} câu hỏi
                        </span>
                        <span>
                            <i class="bi bi-check-circle"></i>
                            ${attempt.correctCount} đúng
                        </span>
                    </div>
                </div>
                <div class="history-item-score">
                    <div class="history-score-value">${score}<span style="font-size: 1rem; color: #9ca3af;">/10</span></div>
                    <div class="history-score-badge ${badgeClass}">${badgeText}</div>
                </div>
            </div>
        `;
    }).join('');
}

function renderAllHistory() {
    renderHistory(null); // Show all
}

function filterByMonth() {
    const now = new Date();
    const firstDayOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    
    const filteredAttempts = allAttempts.filter(a => {
        return new Date(a.finishedAt) >= firstDayOfMonth;
    });

    // Temporarily replace allAttempts for rendering
    const originalAttempts = allAttempts;
    allAttempts = filteredAttempts;
    renderHistory();
    allAttempts = originalAttempts;
}

function viewAttemptDetail(quizSetId, attemptId) {
    // For now, just redirect to quiz take page
    // In the future, you can create a detailed result page
    window.location.href = `/html/quiz-take.html?setId=${quizSetId}`;
}

function getTimeAgo(date) {
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Vừa xong';
    if (diffMins < 60) return `${diffMins} phút trước`;
    if (diffHours < 24) return `${diffHours} giờ trước`;
    if (diffDays < 7) return `${diffDays} ngày trước`;
    
    return date.toLocaleDateString('vi-VN');
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

function showError(message) {
    // You can implement a toast notification here
    console.error(message);
}
