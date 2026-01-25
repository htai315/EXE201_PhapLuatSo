/**
 * Auth Guard - Centralized authentication check for protected pages
 * 
 * Uses TokenManager for in-memory access token storage.
 * Refresh token is stored as HttpOnly cookie (invisible to JS).
 * 
 * Usage:
 * 1. Include token-manager.js BEFORE this script
 * 2. Add data-auth-required="true" to <body> for pages that require login
 * 3. Add data-admin-required="true" to <body> for admin-only pages
 */

const AUTH = {
    // Storage keys (only for userName/userRole, NOT tokens)
    KEYS: {
        USER_NAME: 'userName',
        USER_ROLE: 'userRole'
    },

    // Get access token from memory
    getToken() {
        return window.TokenManager?.getAccessToken();
    },

    // Check if user is logged in (has token in memory)
    isLoggedIn() {
        return window.TokenManager?.isAuthenticated() || false;
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

    // Save auth data after login (only userName/role, NOT tokens)
    saveUserInfo(userName, role) {
        if (userName) {
            localStorage.setItem(this.KEYS.USER_NAME, userName);
        }
        if (role) {
            localStorage.setItem(this.KEYS.USER_ROLE, role);
        }
    },

    // Clear all auth data (logout)
    clearAuth() {
        window.TokenManager?.clearAccessToken();
        localStorage.removeItem(this.KEYS.USER_NAME);
        localStorage.removeItem(this.KEYS.USER_ROLE);
        // Clear any legacy localStorage items
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
    },

    // Redirect to login page
    redirectToLogin() {
        const currentPath = window.location.pathname;
        if (!currentPath.includes('/html/login.html')) {
            sessionStorage.setItem('redirectAfterLogin', window.location.href);
            window.location.href = '/html/login.html';
        }
    },

    // Redirect to home page
    redirectToHome() {
        window.location.href = '/index.html';
    },

    // Get redirect URL after login
    getRedirectUrl() {
        const url = sessionStorage.getItem('redirectAfterLogin');
        sessionStorage.removeItem('redirectAfterLogin');
        return url;
    },

    // Verify token with server
    async verifyToken() {
        const token = this.getToken();
        if (!token) return false;

        try {
            const client = AppRuntime.getClient();
            if (!client) return false;
            // Use centralized AppRuntime.getMe to validate token and fetch user info
            try {
                const me = await AppRuntime.safe('AuthGuard:verifyToken', () => AppRuntime.getMe(client));
                return !!me;
            } catch (e) {
                return false;
            }
        } catch (error) {
            console.error('Token verification failed:', error);
            return false;
        }
    },

    // Refresh access token (uses TokenManager which uses cookie)
    async refreshToken() {
        return await window.TokenManager?.refreshAccessToken() || false;
    },

    // Main guard function - call this to protect a page
    async guard(options = {}) {
        const {
            requireAuth = true,
            requireAdmin = false,
            redirect = true,
            verifyWithServer = false
        } = options;

        // Try to get token or rehydrate from cookie
        let isLoggedIn = this.isLoggedIn();

        if (!isLoggedIn && requireAuth) {
            // Try rehydration
            console.log('[AUTH Guard] No token in memory, attempting rehydration...');
            const refreshed = await this.refreshToken();
            isLoggedIn = refreshed;
        }

        // Check if auth is required but user not logged in
        if (requireAuth && !isLoggedIn) {
            if (redirect) {
                this.redirectToLogin();
            }
            return false;
        }

        // If admin is required, fetch role from server if needed
        if (requireAdmin && !this.getUserRole()) {
            console.log('[AUTH Guard] Admin required but no role in localStorage, fetching from server...');
            try {
                const client = AppRuntime.getClient();
                if (client) {
                    const user = await AppRuntime.safe('AuthGuard:getMe', () => AppRuntime.getMe(client));
                    if (user) {
                        console.log('[AUTH Guard] Fetched user role:', user.role);
                        this.saveUserInfo(user.fullName, user.role);
                    }
                } else {
                    console.log('[AUTH Guard] No API client available to fetch user role');
                }
            } catch (error) {
                console.error('Failed to fetch user role:', error);
            }
        }

        // Check admin role
        const savedRole = this.getUserRole();
        console.log('[AUTH Guard] Checking admin. savedRole:', savedRole, 'isAdmin:', this.isAdmin());

        if (requireAdmin && !this.isAdmin()) {
            console.log('[AUTH Guard] Admin required but user is not admin, redirecting to home...');
            if (redirect) {
                this.redirectToHome();
            }
            return false;
        }

        // Optionally verify with server
        if (verifyWithServer) {
            const isValid = await this.verifyToken();
            if (!isValid) {
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

    const requireAuth = body.dataset.authRequired === 'true';
    const requireAdmin = body.dataset.adminRequired === 'true';
    const redirect = body.dataset.authRedirect !== 'false';
    const verifyWithServer = body.dataset.authVerify === 'true';

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

