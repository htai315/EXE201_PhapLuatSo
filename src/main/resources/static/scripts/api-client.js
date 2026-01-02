/**
 * API Client với Auto Token Refresh
 * 
 * Tự động refresh access token khi hết hạn (401 Unauthorized)
 * và retry request ban đầu với token mới
 */

const API_CLIENT = {
    /**
     * Fetch với auto token refresh
     * @param {string} url - API endpoint
     * @param {object} options - Fetch options
     * @returns {Promise<Response>}
     */
    async fetchWithAuth(url, options = {}) {
        const token = localStorage.getItem('accessToken');
        
        // Add Authorization header
        const headers = {
            ...options.headers,
            'Authorization': token ? `Bearer ${token}` : ''
        };

        // First attempt
        let response = await fetch(url, {
            ...options,
            headers
        });

        // Nếu 403 Forbidden với ACCOUNT_BANNED → hiển thị thông báo và logout
        if (response.status === 403) {
            try {
                const clonedResponse = response.clone();
                const data = await clonedResponse.json();
                if (data.error === 'ACCOUNT_BANNED') {
                    this.handleAccountBanned(data.message);
                    return response;
                }
            } catch (e) {
                // Not JSON or other error, continue normal flow
            }
        }

        // Nếu 401 Unauthorized → thử refresh token
        if (response.status === 401) {
            console.log('Access token expired, attempting refresh...');
            
            const refreshSuccess = await this.refreshToken();
            
            if (refreshSuccess) {
                // Retry request với token mới
                const newToken = localStorage.getItem('accessToken');
                response = await fetch(url, {
                    ...options,
                    headers: {
                        ...options.headers,
                        'Authorization': `Bearer ${newToken}`
                    }
                });
            } else {
                // Refresh failed → redirect to login
                console.error('Token refresh failed, redirecting to login');
                this.redirectToLogin();
            }
        }

        return response;
    },

    /**
     * Handle account banned - show toast message and logout
     * @param {string} message - Ban message from server
     */
    handleAccountBanned(message) {
        // Prevent multiple calls
        if (this._handlingBan) return;
        this._handlingBan = true;
        
        // Clear tokens
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userName');
        
        const banMessage = message || 'Tài khoản của bạn đã bị khóa.';
        
        // Use existing Toast component with same style as other toasts
        this.showBanToast(banMessage);
        
        // Redirect to login after 2 seconds
        setTimeout(() => {
            this._handlingBan = false;
            window.location.href = '/html/login.html';
        }, 2000);
    },
    
    /**
     * Show ban toast notification using same style as existing toasts
     */
    showBanToast(message) {
        // Try to use existing Toast component first
        if (window.Toast && typeof window.Toast.error === 'function') {
            window.Toast.error(message, 2000);
            return;
        }
        
        // Fallback: Create toast with same style as toast-notification.css
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container';
            container.style.cssText = 'position:fixed;top:20px;right:20px;z-index:9999;display:flex;flex-direction:column;gap:12px;pointer-events:none;';
            document.body.appendChild(container);
        }
        
        const toast = document.createElement('div');
        toast.className = 'toast toast-error';
        toast.style.cssText = `
            display:flex;align-items:center;gap:12px;min-width:320px;max-width:420px;
            padding:16px 20px;background:linear-gradient(135deg,#ffffff 0%,#fef2f2 100%);
            border-radius:12px;box-shadow:0 8px 24px rgba(0,0,0,0.15);
            border-left:4px solid #ef4444;pointer-events:auto;
            opacity:0;transform:translateX(400px);transition:all 0.3s cubic-bezier(0.4,0,0.2,1);
        `;
        
        toast.innerHTML = `
            <div class="toast-icon" style="flex-shrink:0;width:40px;height:40px;display:flex;align-items:center;justify-content:center;border-radius:10px;background:linear-gradient(135deg,#ef4444 0%,#dc2626 100%);color:white;">
                <i class="bi bi-x-circle-fill" style="font-size:1.25rem;"></i>
            </div>
            <div class="toast-content" style="flex:1;min-width:0;">
                <div class="toast-message" style="font-size:0.95rem;font-weight:500;color:#0f172a;line-height:1.5;">${message}</div>
            </div>
        `;
        
        container.appendChild(toast);
        
        // Trigger animation
        setTimeout(() => {
            toast.style.opacity = '1';
            toast.style.transform = 'translateX(0)';
        }, 10);
        
        // Auto remove
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(400px)';
            setTimeout(() => toast.remove(), 300);
        }, 2000);
    },

    /**
     * Parse error response from API
     * @param {Response} response - Fetch response
     * @returns {Promise<Object>} - Error object with message
     */
    async parseErrorResponse(response) {
        let errorMessage = 'Đã xảy ra lỗi';
        try {
            const data = await response.json();
            errorMessage = data.error || data.message || errorMessage;
            // Return error object with extracted message
            return {
                error: errorMessage,
                code: data.code,
                status: response.status
            };
        } catch (e) {
            // If response is not JSON, use status text
            return {
                error: response.statusText || errorMessage,
                status: response.status
            };
        }
    },

    /**
     * Refresh access token
     * @returns {Promise<boolean>} - true nếu refresh thành công
     */
    async refreshToken() {
        const refreshToken = localStorage.getItem('refreshToken');
        
        if (!refreshToken) {
            console.error('No refresh token found');
            return false;
        }

        try {
            const response = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            });

            if (response.ok) {
                const data = await response.json();
                
                // Lưu tokens mới
                localStorage.setItem('accessToken', data.accessToken);
                if (data.refreshToken) {
                    localStorage.setItem('refreshToken', data.refreshToken);
                }
                
                console.log('Token refreshed successfully');
                return true;
            } else {
                console.error('Refresh token invalid or expired');
                return false;
            }
        } catch (error) {
            console.error('Error refreshing token:', error);
            return false;
        }
    },

    /**
     * Redirect to login page
     */
    redirectToLogin() {
        // Clear tokens
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userName');
        
        // Redirect
        const currentPath = window.location.pathname;
        if (!currentPath.includes('/html/login.html')) {
            window.location.href = '/html/login.html';
        }
    },

    /**
     * Helper: GET request with error handling
     */
    async get(url) {
        const response = await this.fetchWithAuth(url, {
            method: 'GET'
        });
        
        if (!response.ok) {
            const error = await this.parseErrorResponse(response);
            throw error;
        }
        
        return response.json();
    },

    /**
     * Helper: POST request with error handling
     */
    async post(url, data) {
        const response = await this.fetchWithAuth(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            const error = await this.parseErrorResponse(response);
            throw error;
        }
        
        // Handle empty response (201 Created with no body, etc.)
        const text = await response.text();
        if (!text) {
            return null;
        }
        
        try {
            return JSON.parse(text);
        } catch (e) {
            return text;
        }
    },

    /**
     * Helper: PUT request with error handling
     */
    async put(url, data) {
        const response = await this.fetchWithAuth(url, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            const error = await this.parseErrorResponse(response);
            throw error;
        }
        
        // Handle empty response (204 No Content or empty body)
        const text = await response.text();
        if (!text) {
            return null;
        }
        
        try {
            return JSON.parse(text);
        } catch (e) {
            return text;
        }
    },

    /**
     * Helper: DELETE request with error handling
     */
    async delete(url) {
        const response = await this.fetchWithAuth(url, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            const error = await this.parseErrorResponse(response);
            throw error;
        }
        
        // Handle empty response (204 No Content or empty body)
        const text = await response.text();
        if (!text) {
            return null;
        }
        
        try {
            return JSON.parse(text);
        } catch (e) {
            return text;
        }
    }
};

// Export cho global scope
window.apiClient = API_CLIENT;
window.API_CLIENT = API_CLIENT; // Backward compatibility
