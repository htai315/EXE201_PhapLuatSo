/**
 * API Client with In-Memory Token + HttpOnly Cookie Refresh
 * 
 * Security model:
 * - Access token: stored in memory via TokenManager (XSS-safe)
 * - Refresh token: HttpOnly cookie (auto-sent by browser)
 * 
 * On 401: calls TokenManager.refreshAccessToken() which uses the cookie
 */

const API_CLIENT = {
    /**
     * Fetch with auto token refresh
     * @param {string} url - API endpoint
     * @param {object} options - Fetch options
     * @returns {Promise<Response>}
     */
    async fetchWithAuth(url, options = {}) {
        // Get token from memory (not localStorage)
        const token = window.TokenManager?.getAccessToken();

        // Add Authorization header and credentials for cookies
        const headers = {
            ...options.headers,
            'Authorization': token ? `Bearer ${token}` : ''
        };

        // First attempt - include credentials for cookie handling
        let response = await fetch(url, {
            ...options,
            headers,
            credentials: 'include' // CRITICAL: send cookies
        });

        // Handle 403 Forbidden with ACCOUNT_BANNED
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

        // Handle 401 Unauthorized - try refresh
        if (response.status === 401) {
            // Skip refresh for refresh endpoint itself (prevent infinite loop)
            if (url.includes('/api/auth/refresh')) {
                return response;
            }

            console.log('[API Client] Access token expired, attempting refresh...');

            const refreshSuccess = await window.TokenManager?.refreshAccessToken();

            if (refreshSuccess) {
                // Retry request with new token from memory
                const newToken = window.TokenManager?.getAccessToken();
                response = await fetch(url, {
                    ...options,
                    headers: {
                        ...options.headers,
                        'Authorization': `Bearer ${newToken}`
                    },
                    credentials: 'include'
                });
            } else {
                // Refresh failed → redirect to login
                console.error('[API Client] Token refresh failed, redirecting to login');
                this.redirectToLogin();
            }
        }

        // Handle 429 Too Many Requests (rate limit/session limit)
        if (response.status === 429) {
            try {
                const errorData = await response.json();
                console.warn('[API Client] Rate limit/session limit exceeded:', errorData);

                // Show user-friendly error message with Toast if available
                if (window.Toast) {
                    if (errorData.code === 'SESSION_LIMIT_EXCEEDED') {
                        window.Toast.warning(errorData.error || 'Phiên chat đã đạt 10 câu hỏi. Vui lòng tạo phiên mới.');
                    } else {
                        window.Toast.error(errorData.error || 'Quá nhiều yêu cầu. Vui lòng thử lại sau.');
                    }
                } else if (window.ERROR_HANDLER) {
                    window.ERROR_HANDLER.showErrorAlert(errorData.error || 'Quá nhiều yêu cầu. Vui lòng thử lại sau.');
                }

                // Throw error so caller can also handle
                throw { ...errorData, status: 429 };
            } catch (e) {
                if (e.status === 429) throw e; // Re-throw our custom error
                console.error('[API Client] Failed to parse 429 response:', e);
            }
        }

        return response;
    },

    /**
     * Handle account banned - show toast and logout
     */
    handleAccountBanned(message) {
        if (this._handlingBan) return;
        this._handlingBan = true;

        // Clear tokens via TokenManager
        window.TokenManager?.clearAccessToken();

        const banMessage = message || 'Tài khoản của bạn đã bị khóa.';
        this.showBanToast(banMessage);

        setTimeout(() => {
            this._handlingBan = false;
            window.location.href = '/html/login.html';
        }, 2000);
    },

    /**
     * Show ban toast notification
     */
    showBanToast(message) {
        if (window.Toast && typeof window.Toast.error === 'function') {
            window.Toast.error(message, 2000);
            return;
        }

        // Fallback toast
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
        setTimeout(() => {
            toast.style.opacity = '1';
            toast.style.transform = 'translateX(0)';
        }, 10);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(400px)';
            setTimeout(() => toast.remove(), 300);
        }, 2000);
    },

    /**
     * Parse error response from API
     */
    async parseErrorResponse(response) {
        let errorMessage = 'Đã xảy ra lỗi';
        try {
            const data = await response.json();
            errorMessage = data.error || data.message || errorMessage;
            return {
                error: errorMessage,
                code: data.code,
                status: response.status
            };
        } catch (e) {
            return {
                error: response.statusText || errorMessage,
                status: response.status
            };
        }
    },

    /**
     * Redirect to login page
     */
    redirectToLogin() {
        window.TokenManager?.clearAccessToken();

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

        const text = await response.text();
        if (!text) return null;

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

        const text = await response.text();
        if (!text) return null;

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

        const text = await response.text();
        if (!text) return null;

        try {
            return JSON.parse(text);
        } catch (e) {
            return text;
        }
    },

    /**
     * Helper: Upload file with FormData
     */
    async upload(url, formData) {
        const response = await this.fetchWithAuth(url, {
            method: 'POST',
            body: formData
            // Don't set Content-Type header - browser will set it with boundary
        });

        if (!response.ok) {
            const error = await this.parseErrorResponse(response);
            throw error;
        }

        const text = await response.text();
        if (!text) return null;

        try {
            return JSON.parse(text);
        } catch (e) {
            return text;
        }
    }
};

// Export for global scope
window.apiClient = API_CLIENT;
// Global auth ready promise - resolves when auth rehydration completes
window.authReady = (async function() {
    try {
        console.debug('[authReady] starting');
        // Prefer TokenManager refresh so TokenManager holds the access token in memory after resolve
        if (window.TokenManager && typeof window.TokenManager.refreshAccessToken === 'function') {
            console.debug('[authReady] TokenManager detected, attempting refresh...');
            try {
                const refreshed = await window.TokenManager.refreshAccessToken();
                const isAuthenticated = !!(window.TokenManager && window.TokenManager.isAuthenticated && window.TokenManager.isAuthenticated());
                console.debug('[authReady] refresh result:', refreshed, 'isAuthenticated:', isAuthenticated);
                return isAuthenticated;
            } catch (e) {
                console.warn('[authReady] TokenManager.refreshAccessToken() failed:', e && e.message ? e.message : e);
                // fallback to AUTH.guard below
            }
        }

        // Fallback: use AUTH.guard if available
        if (typeof AUTH !== 'undefined' && typeof AUTH.guard === 'function') {
            console.debug('[authReady] falling back to AUTH.guard');
            const isReady = await AUTH.guard({ requireAuth: false, redirect: false });
            console.debug('[authReady] AUTH.guard result:', isReady);
            return isReady;
        }

        // No auth system available
        console.debug('[authReady] no TokenManager or AUTH available; resolving false');
        return false;
    } catch (error) {
        console.warn('[authReady] Auth rehydration failed:', error && error.message ? error.message : error);
        return false;
    }
})();

// Note: do NOT assign to `window.API_CLIENT` here because `API_CLIENT` is declared as
// a top-level `const` which creates a non-writable global property. Assigning
// `window.API_CLIENT = API_CLIENT` would attempt to write that readonly property
// and triggers an error in strict environments / some editors.
