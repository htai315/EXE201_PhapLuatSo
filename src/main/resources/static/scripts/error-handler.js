/**
 * Global Error Handler & Error Boundary
 * 
 * Xử lý tất cả errors trong app để tránh crash
 * và hiển thị thông báo thân thiện cho user
 */

const ERROR_HANDLER = {
    /**
     * Initialize error handlers
     */
    init() {
        // Global error handler cho uncaught errors
        window.addEventListener('error', (event) => {
            console.error('Global error caught:', event.error);
            this.handleError(event.error, 'Đã xảy ra lỗi không mong muốn');
            event.preventDefault(); // Prevent default browser error handling
        });

        // Promise rejection handler
        window.addEventListener('unhandledrejection', (event) => {
            console.error('Unhandled promise rejection:', event.reason);
            this.handleError(event.reason, 'Đã xảy ra lỗi khi xử lý yêu cầu');
            event.preventDefault();
        });

        console.log('Error handlers initialized');
    },

    /**
     * Handle error và hiển thị thông báo
     * @param {Error} error - Error object
     * @param {string} userMessage - Message hiển thị cho user
     */
    handleError(error, userMessage = 'Đã xảy ra lỗi') {
        // Log chi tiết error cho debugging
        console.error('Error details:', {
            message: error?.message,
            stack: error?.stack,
            timestamp: new Date().toISOString()
        });

        // Hiển thị thông báo cho user
        this.showErrorAlert(userMessage);

        // Optional: Send error to logging service (Sentry, LogRocket, etc.)
        // this.sendToLoggingService(error);
    },

    /**
     * Hiển thị error alert
     * @param {string} message - Error message
     */
    showErrorAlert(message) {
        // Tìm hoặc tạo alert container
        let alertContainer = document.getElementById('globalAlertContainer');
        
        if (!alertContainer) {
            alertContainer = document.createElement('div');
            alertContainer.id = 'globalAlertContainer';
            alertContainer.style.cssText = `
                position: fixed;
                top: 80px;
                right: 20px;
                z-index: 9999;
                max-width: 400px;
            `;
            document.body.appendChild(alertContainer);
        }

        // Tạo alert element
        const alertId = 'alert-' + Date.now();
        const alertHtml = `
            <div id="${alertId}" class="alert alert-danger alert-dismissible fade show shadow-lg" role="alert" style="margin-bottom: 10px;">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                <strong>Lỗi:</strong> ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;

        alertContainer.insertAdjacentHTML('beforeend', alertHtml);

        // Auto dismiss sau 5 giây
        setTimeout(() => {
            const alertElement = document.getElementById(alertId);
            if (alertElement) {
                const bsAlert = bootstrap.Alert.getOrCreateInstance(alertElement);
                bsAlert.close();
            }
        }, 5000);
    },

    /**
     * Wrap async function với error handling
     * @param {Function} fn - Async function
     * @returns {Function} - Wrapped function
     */
    wrapAsync(fn) {
        return async (...args) => {
            try {
                return await fn(...args);
            } catch (error) {
                this.handleError(error, 'Đã xảy ra lỗi khi thực hiện thao tác');
                throw error; // Re-throw để caller có thể handle nếu cần
            }
        };
    },

    /**
     * Safe fetch wrapper
     * @param {string} url - URL
     * @param {object} options - Fetch options
     * @returns {Promise<Response>}
     */
    async safeFetch(url, options = {}) {
        try {
            const response = await fetch(url, options);
            
            // Check HTTP errors
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText || response.statusText}`);
            }
            
            return response;
        } catch (error) {
            this.handleError(error, 'Không thể kết nối đến máy chủ');
            throw error;
        }
    },

    /**
     * Show loading state
     * @param {boolean} show - Show or hide
     */
    showLoading(show = true) {
        let loader = document.getElementById('globalLoader');
        
        if (!loader) {
            loader = document.createElement('div');
            loader.id = 'globalLoader';
            loader.innerHTML = `
                <div style="
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    background: rgba(0, 0, 0, 0.5);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    z-index: 9998;
                ">
                    <div class="spinner-border text-light" role="status" style="width: 3rem; height: 3rem;">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </div>
            `;
            document.body.appendChild(loader);
        }
        
        loader.style.display = show ? 'block' : 'none';
    },

    /**
     * Optional: Send error to logging service
     * @param {Error} error - Error object
     */
    sendToLoggingService(error) {
        // TODO: Implement logging service integration
        // Example: Sentry.captureException(error);
    }
};

// Initialize khi DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => ERROR_HANDLER.init());
} else {
    ERROR_HANDLER.init();
}

// Export cho global scope
window.ERROR_HANDLER = ERROR_HANDLER;
