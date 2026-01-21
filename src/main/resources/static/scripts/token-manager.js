/**
 * Token Manager - In-memory access token storage
 * 
 * Security model:
 * - Access token: stored in memory only (lost on reload)
 * - Refresh token: HttpOnly cookie (invisible to JS, auto-sent by browser)
 * 
 * On page load, call refreshAccessToken() to rehydrate from cookie.
 */

(function () {
    'use strict';

    // In-memory storage (not accessible to XSS)
    let accessToken = null;
    let tokenExpiresAt = null;
    let refreshPromise = null; // Mutex for refresh requests

    /**
     * Store access token in memory
     * @param {string} token - Access token
     * @param {number} expiresIn - Seconds until expiry
     */
    function setAccessToken(token, expiresIn) {
        accessToken = token;
        tokenExpiresAt = Date.now() + (expiresIn * 1000);
        console.log('[TokenManager] Access token set, expires in', expiresIn, 'seconds');
    }

    /**
     * Get current access token
     * @returns {string|null} Access token or null if not set
     */
    function getAccessToken() {
        return accessToken;
    }

    /**
     * Check if access token is expired or will expire soon
     * @param {number} bufferSeconds - Buffer time before expiry (default 60s)
     * @returns {boolean} True if token is expired or will expire within buffer
     */
    function isTokenExpired(bufferSeconds) {
        bufferSeconds = bufferSeconds || 60;
        if (!accessToken || !tokenExpiresAt) {
            return true;
        }
        return Date.now() >= (tokenExpiresAt - bufferSeconds * 1000);
    }

    /**
     * Check if user is authenticated (has valid token)
     * @returns {boolean}
     */
    function isAuthenticated() {
        return accessToken !== null && !isTokenExpired();
    }

    /**
     * Clear access token from memory
     */
    function clearAccessToken() {
        accessToken = null;
        tokenExpiresAt = null;
        console.log('[TokenManager] Access token cleared');
    }

    /**
     * Refresh access token using HttpOnly cookie.
     * Implements mutex to prevent concurrent refresh calls.
     * 
     * @returns {Promise<boolean>} True if refresh succeeded
     */
    async function refreshAccessToken() {
        // Reuse in-flight refresh request (mutex)
        if (refreshPromise) {
            console.log('[TokenManager] Reusing existing refresh request');
            return refreshPromise;
        }

        console.log('[TokenManager] Starting refresh request');

        refreshPromise = (async function () {
            try {
                const response = await fetch('/api/auth/refresh', {
                    method: 'POST',
                    credentials: 'include', // CRITICAL: send cookies
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });

                if (response.ok) {
                    const data = await response.json();
                    setAccessToken(data.accessToken, data.expiresIn);
                    console.log('[TokenManager] Refresh successful');
                    return true;
                } else {
                    console.log('[TokenManager] Refresh failed with status', response.status);
                    clearAccessToken();
                    return false;
                }
            } catch (error) {
                console.error('[TokenManager] Refresh error:', error);
                clearAccessToken();
                return false;
            } finally {
                refreshPromise = null; // Clear mutex
            }
        })();

        return refreshPromise;
    }

    /**
     * Logout - clear local state and call logout API
     * @returns {Promise<void>}
     */
    async function logout() {
        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'include'
            });
        } catch (error) {
            console.warn('[TokenManager] Logout API error:', error);
        }

        clearAccessToken();
        // Also clear any legacy localStorage items
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userName');
        localStorage.removeItem('userRole');
    }

    // Export for global use
    window.TokenManager = {
        setAccessToken: setAccessToken,
        getAccessToken: getAccessToken,
        isTokenExpired: isTokenExpired,
        isAuthenticated: isAuthenticated,
        clearAccessToken: clearAccessToken,
        refreshAccessToken: refreshAccessToken,
        logout: logout
    };
})();
