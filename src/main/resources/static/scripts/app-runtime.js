/**
 * AppRuntime - helper utilities for DOM/auth/bootstrap and API client access
 *
 * Purpose:
 * - Provide a single place to await DOM + auth readiness
 * - Provide a canonical way to obtain the API client (window.apiClient)
 * - Provide safe wrappers for calling /api/auth/me and admin checks
 *
 * Notes:
 * - DO NOT rely on `window.API_CLIENT` (forbidden) because assigning to that
 *   identifier can trigger readonly-assignment errors in some environments.
 * - The canonical public client is `window.apiClient` which api-client.js sets.
 */
(function () {
    'use strict';

    function domReady() {
        if (document.readyState !== 'loading') return Promise.resolve();
        return new Promise(resolve => document.addEventListener('DOMContentLoaded', resolve));
    }

    async function authReady() {
        if (window.authReady && typeof window.authReady.then === 'function') {
            // authReady is created by api-client.js and resolves once rehydration completes
            try {
                return await window.authReady;
            } catch (e) {
                // bubble up - callers may choose how to handle
                throw e;
            }
        }
        // No auth system present - resolve as false
        return false;
    }

    function getClient() {
        // Preferred canonical global
        if (window.apiClient) return window.apiClient;
        // Fallback to in-page const if present (rare)
        if (typeof API_CLIENT !== 'undefined') return API_CLIENT;
        return null;
    }

    function requireClient(context) {
        const c = getClient();
        if (!c) {
            const err = new Error(`[AppRuntime] API client missing in ${context || 'unknown'}`);
            console.error(err.message);
            throw err;
        }
        return c;
    }

    async function safe(label, fn) {
        try {
            return await fn();
        } catch (e) {
            console.error(`[AppRuntime:${label}]`, e);
            throw e;
        }
    }

    async function getMe(client) {
        const c = client || getClient();
        if (!c) {
            throw new Error('[AppRuntime.getMe] API client not available');
        }
        return await c.get('/api/auth/me');
    }

    async function isAuthenticated(client) {
        try {
            const c = client || getClient();
            if (!c) return false;
            const me = await getMe(c);
            return !!me;
        } catch (e) {
            return false;
        }
    }

    function isAdmin(me) {
        if (!me) return false;
        if (me.role === 'ADMIN' || me.role === 'ROLE_ADMIN') return true;
        if (Array.isArray(me.roles)) {
            return me.roles.includes('ADMIN') || me.roles.includes('ROLE_ADMIN');
        }
        return false;
    }

    async function ensureAdmin(client) {
        try {
            const c = client || getClient();
            if (!c) {
                console.warn('[AppRuntime.ensureAdmin] client missing');
                return null;
            }
            const me = await getMe(c);
            if (!isAdmin(me)) {
                console.warn('[AppRuntime.ensureAdmin] user is not admin');
                return null;
            }
            return me;
        } catch (e) {
            console.warn('[AppRuntime.ensureAdmin] failed to verify admin', e);
            return null;
        }
    }

    // Expose globally
    window.AppRuntime = {
        domReady,
        authReady,
        getClient,
        requireClient,
        safe,
        getMe,
        isAuthenticated,
        isAdmin,
        ensureAdmin
    };

})();


