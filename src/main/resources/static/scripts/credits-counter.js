/**
 * Credits Counter Component
 * Displays remaining credits in navbar
 * Auto-refreshes after each usage
 */

class CreditsCounter {
    constructor() {
        this.credits = null;
        this.container = null;
    }

    /**
     * Initialize credits counter
     * @param {string} containerId - ID of container element
     * @param {string} type - 'chat' or 'quiz_gen'
     */
    async init(containerId, type = 'chat') {
        this.container = document.getElementById(containerId);
        if (!this.container) {
            console.warn('Credits counter container not found:', containerId);
            return;
        }

        this.type = type;
        await this.fetchCredits();
        this.render();
    }

    /**
     * Fetch credits from API
     */
    async fetchCredits() {
        try {
            const client = AppRuntime.getClient();
            if (!client) {
                console.warn('[CreditsCounter] API client not available; cannot fetch credits');
                this.credits = null;
                return;
            }
            this.credits = await AppRuntime.safe('CreditsCounter:getBalance', () => client.get('/api/credits/balance'));
        } catch (error) {
            console.error('Error fetching credits:', error);
            this.credits = null;
        }
    }

    /**
     * Render credits counter
     */
    render() {
        if (!this.container || !this.credits) {
            return;
        }

        const isChat = this.type === 'chat';
        const remaining = isChat ? this.credits.chatCredits : this.credits.quizGenCredits;
        const icon = isChat ? '💬' : '🤖';
        const label = isChat ? 'Chat' : 'AI Tạo Đề';

        // Determine color based on remaining credits
        let colorClass = 'text-success';
        if (remaining <= 0) {
            colorClass = 'text-danger';
        } else if (remaining <= 3) {
            colorClass = 'text-warning';
        }

        // Check if expired
        const isExpired = this.credits.isExpired;
        const expiryWarning = isExpired ? '<small class="text-danger d-block">Đã hết hạn</small>' : '';

        this.container.innerHTML = `
            <div class="credits-counter ${colorClass}">
                <span class="credits-icon">${icon}</span>
                <span class="credits-text">
                    <strong>${remaining}</strong> lượt ${label}
                </span>
                ${expiryWarning}
            </div>
        `;

        // Show warning if low credits
        if (remaining > 0 && remaining <= 3 && !isExpired) {
            this.showLowCreditsWarning(remaining);
        }

        // Show upgrade modal if no credits
        if (remaining <= 0 || isExpired) {
            this.showUpgradeModal();
        }
    }

    /**
     * Show low credits warning
     */
    showLowCreditsWarning(remaining) {
        // Only show once per session
        const warningKey = `credits_warning_shown_${this.type}`;
        if (sessionStorage.getItem(warningKey)) {
            return;
        }

        const message = `Bạn chỉ còn ${remaining} lượt ${this.type === 'chat' ? 'chat' : 'AI tạo đề'}. Hãy nâng cấp để tiếp tục sử dụng!`;
        
        // Show toast notification
        this.showToast(message, 'warning');
        sessionStorage.setItem(warningKey, 'true');
    }

    /**
     * Show upgrade modal when out of credits
     */
    showUpgradeModal() {
        // Check if modal already shown
        if (document.getElementById('upgradeModal')) {
            return;
        }

        const isChat = this.type === 'chat';
        const feature = isChat ? 'chat AI' : 'AI tạo đề';
        const expiredText = this.credits?.isExpired ? 'Credits của bạn đã hết hạn.' : `Bạn đã hết lượt ${feature}.`;

        const modal = document.createElement('div');
        modal.id = 'upgradeModal';
        modal.className = 'modal fade';
        modal.innerHTML = `
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header bg-primary text-white">
                        <h5 class="modal-title">
                            <i class="bi bi-star-fill"></i> Nâng Cấp Tài Khoản
                        </h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body text-center py-4">
                        <div class="mb-3">
                            <i class="bi bi-exclamation-circle text-warning" style="font-size: 3rem;"></i>
                        </div>
                        <h5 class="mb-3">${expiredText}</h5>
                        <p class="text-muted">
                            Nâng cấp lên gói <strong>REGULAR</strong> hoặc <strong>STUDENT</strong> 
                            để tiếp tục sử dụng dịch vụ.
                        </p>
                        <div class="d-grid gap-2 mt-4">
                            <a href="/html/plans.html" class="btn btn-primary btn-lg">
                                <i class="bi bi-cart-plus"></i> Xem Các Gói
                            </a>
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">
                                Để Sau
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();

        // Remove modal from DOM when hidden
        modal.addEventListener('hidden.bs.modal', () => {
            modal.remove();
        });
    }

    /**
     * Show toast notification
     */
    showToast(message, type = 'info') {
        // Create toast container if not exists
        let toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = '9999';
            document.body.appendChild(toastContainer);
        }

        const bgClass = type === 'warning' ? 'bg-warning' : 'bg-info';
        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-white ${bgClass} border-0`;
        toast.setAttribute('role', 'alert');
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;

        toastContainer.appendChild(toast);
        const bsToast = new bootstrap.Toast(toast, { delay: 5000 });
        bsToast.show();

        // Remove toast after hidden
        toast.addEventListener('hidden.bs.toast', () => {
            toast.remove();
        });
    }

    /**
     * Refresh credits (call after usage)
     */
    async refresh() {
        await this.fetchCredits();
        this.render();
    }
}

// Export for use in other scripts
window.CreditsCounter = CreditsCounter;
