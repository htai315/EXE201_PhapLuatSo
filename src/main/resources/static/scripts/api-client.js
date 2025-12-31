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
        
        return response.json();
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
        
        return response.json();
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
        
        return response.json();
    }
};

// Export cho global scope
window.apiClient = API_CLIENT;
window.API_CLIENT = API_CLIENT; // Backward compatibility
