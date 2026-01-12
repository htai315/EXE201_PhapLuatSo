/**
 * Auth Guard - Centralized authentication check for protected pages
 * 
 * Usage:
 * 1. Include this script in your HTML: <script src="/scripts/auth-guard.js"></script>
 * 2. Add data-auth-required="true" to <body> for pages that require login
 * 3. Add data-auth-redirect="false" to <body> to skip redirect (just check)
 * 4. Add data-admin-required="true" to <body> for admin-only pages
 * 
 * The script will:
 * - Check if user has valid access token
 * - Redirect to login page if not authenticated
 * - Optionally verify token with server
 * - Provide global AUTH object for other scripts to use
 */

const AUTH = {
    // Storage keys
    KEYS: {
        ACCESS_TOKEN: 'accessToken',
        REFRESH_TOKEN: 'refreshToken',
        USER_NAME: 'userName',
        USER_ROLE: 'userRole'
    },

    // Get access token
    getToken() {
        return localStorage.getItem(this.KEYS.ACCESS_TOKEN);
    },

    // Get refresh token
    getRefreshToken() {
        return localStorage.getItem(this.KEYS.REFRESH_TOKEN);
    },

    // Check if user is logged in (has token)
    isLoggedIn() {
        return !!this.getToken();
    },

    // Get user name
    getUserName() {
        return localStorage.getItem(this.KEYS.USER_NAME);
    },

    // Get user role
    getUserRole() {
        return localStorage.getItem(this.KEYS.USER_ROLE);
    },

    // Check if user is admin
    isAdmin() {
        const role = this.getUserRole();
        return role === 'ROLE_ADMIN' || role === 'ADMIN';
    },

    // Save auth data after login
    saveAuth(accessToken, refreshToken, userName, role) {
        localStorage.setItem(this.KEYS.ACCESS_TOKEN, accessToken);
        if (refreshToken) {
            localStorage.setItem(this.KEYS.REFRESH_TOKEN, refreshToken);
        }
        if (userName) {
            localStorage.setItem(this.KEYS.USER_NAME, userName);
        }
        if (role) {
            localStorage.setItem(this.KEYS.USER_ROLE, role);
        }
    },

    // Clear all auth data (logout)
    clearAuth() {
        localStorage.removeItem(this.KEYS.ACCESS_TOKEN);
        localStorage.removeItem(this.KEYS.REFRESH_TOKEN);
        localStorage.removeItem(this.KEYS.USER_NAME);
        localStorage.removeItem(this.KEYS.USER_ROLE);
    },

    // Redirect to login page
    redirectToLogin() {
        const currentPath = window.location.pathname;
        if (!currentPath.includes('/html/login.html')) {
            // Save current URL for redirect after login
            sessionStorage.setItem('redirectAfterLogin', window.location.href);
            window.location.href = '/html/login.html';
        }
    },

    // Redirect to home page (for unauthorized access)
    redirectToHome() {
        window.location.href = '/index.html';
    },

    // Get redirect URL after login
    getRedirectUrl() {
        const url = sessionStorage.getItem('redirectAfterLogin');
        sessionStorage.removeItem('redirectAfterLogin');
        return url;
    },

    // Verify token with server (optional, for extra security)
    async verifyToken() {
        const token = this.getToken();
        if (!token) return false;

        try {
            const response = await fetch('/api/auth/me', {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            return response.ok;
        } catch (error) {
            console.error('Token verification failed:', error);
            return false;
        }
    },

    // Refresh access token
    async refreshToken() {
        const refreshToken = this.getRefreshToken();
        if (!refreshToken) return false;

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
                localStorage.setItem(this.KEYS.ACCESS_TOKEN, data.accessToken);
                if (data.refreshToken) {
                    localStorage.setItem(this.KEYS.REFRESH_TOKEN, data.refreshToken);
                }
                return true;
            }
            return false;
        } catch (error) {
            console.error('Token refresh failed:', error);
            return false;
        }
    },

    // Main guard function - call this to protect a page
    async guard(options = {}) {
        const {
            requireAuth = true,
            requireAdmin = false,
            redirect = true,
            verifyWithServer = false
        } = options;

        // Check if token exists
        if (requireAuth && !this.isLoggedIn()) {
            if (redirect) {
                this.redirectToLogin();
            }
            return false;
        }

        // If admin is required but role is not in localStorage, fetch from server
        if (requireAdmin && !this.getUserRole()) {
            try {
                const response = await fetch('/api/auth/me', {
                    headers: {
                        'Authorization': `Bearer ${this.getToken()}`
                    }
                });
                if (response.ok) {
                    const user = await response.json();
                    if (user.fullName) {
                        localStorage.setItem(this.KEYS.USER_NAME, user.fullName);
                    }
                    if (user.role) {
                        localStorage.setItem(this.KEYS.USER_ROLE, user.role);
                    }
                }
            } catch (error) {
                console.error('Failed to fetch user role:', error);
            }
        }

        // Check admin role
        if (requireAdmin && !this.isAdmin()) {
            if (redirect) {
                this.redirectToHome();
            }
            return false;
        }

        // Optionally verify with server
        if (verifyWithServer) {
            const isValid = await this.verifyToken();
            if (!isValid) {
                // Try refresh
                const refreshed = await this.refreshToken();
                if (!refreshed) {
                    this.clearAuth();
                    if (redirect) {
                        this.redirectToLogin();
                    }
                    return false;
                }
            }
        }

        return true;
    }
};

// Auto-guard based on body data attributes
document.addEventListener('DOMContentLoaded', () => {
    const body = document.body;
    
    // Check data attributes
    const requireAuth = body.dataset.authRequired === 'true';
    const requireAdmin = body.dataset.adminRequired === 'true';
    const redirect = body.dataset.authRedirect !== 'false';
    const verifyWithServer = body.dataset.authVerify === 'true';

    // Only run guard if auth is required
    if (requireAuth || requireAdmin) {
        AUTH.guard({
            requireAuth,
            requireAdmin,
            redirect,
            verifyWithServer
        });
    }
});

// Export for global use
window.AUTH = AUTH;
