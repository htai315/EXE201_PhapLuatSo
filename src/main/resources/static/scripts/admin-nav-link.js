/**
 * Admin Navigation Link
 * Automatically adds "Admin Panel" link to navbar for users with ADMIN role
 */

(function () {
    'use strict';

    // Check if user is admin and add admin link to navbar
    async function addAdminLinkToNavbar() {
        try {
       // Ensure DOM is ready
       await AppRuntime.domReady();

            // Always refresh token to ensure latest auth state
            let hasToken = false;
            if (window.TokenManager && typeof window.TokenManager.refreshAccessToken === 'function') {
                try {
                    console.log('[AdminNav] Refreshing token...');
                    const refreshed = await window.TokenManager.refreshAccessToken();
                    hasToken = !!(window.TokenManager && window.TokenManager.isAuthenticated && window.TokenManager.isAuthenticated());
                    console.log('[AdminNav] Token refresh result:', refreshed, 'hasToken:', hasToken);
                } catch (e) {
                    console.warn('[AdminNav] Token refresh failed:', e?.message || e);
                    hasToken = false;
                }
            }

            if (!hasToken) {
                console.log('[AdminNav] No valid token, skipping admin check');
                return;
            }

            // Resolve client via AppRuntime
            const client = AppRuntime.getClient();
            if (!client) {
                console.warn('[AdminNav] API client not available; cannot check admin role');
                return;
            }

            const user = await AppRuntime.safe('AdminNav:getMe', () => AppRuntime.getMe(client));
            if (!user) {
                console.warn('[AdminNav] user info empty after getMe');
                return;
            }

            if (AppRuntime.isAdmin(user)) {
                insertAdminLink();
            }
        } catch (error) {
            console.error('Error checking admin role:', error);
        }
    }

    // Insert admin link into navbar
    function insertAdminLink() {
        // Check if admin link already exists
        if (document.getElementById('adminNavLink')) return;

        // Find the dropdown menu (auth-only section)
        const dropdownMenu = document.querySelector('.dropdown-menu');
        if (!dropdownMenu) {
            console.log('[AdminNav] dropdown menu not found — observing DOM for insertion');
            // Observe DOM for dropdown-menu insertion (robust vs setTimeout)
            const observer = new MutationObserver((mutations, obs) => {
                const dd = document.querySelector('.dropdown-menu') ||
                           document.querySelector('.navbar .dropdown-menu') ||
                           document.querySelector('[data-navbar-dropdown]');
                if (dd) {
                    console.log('[AdminNav] dropdown menu appeared — inserting admin link');
                    obs.disconnect();
                    insertAdminLink();
                }
            });
            observer.observe(document.body, { childList: true, subtree: true });
            // Fail-safe: stop observing after 10s to avoid leaks
            setTimeout(() => {
                try { observer.disconnect(); } catch (e) {}
            }, 10000);
            return;
        }

        // Create admin link item (same structure as other dropdown items)
        const adminLinkItem = document.createElement('li');
        const adminLink = document.createElement('a');
        adminLink.className = 'dropdown-item';
        adminLink.href = '/html/admin/dashboard.html';
        adminLink.id = 'adminNavLink';
        adminLink.innerHTML = '<i class="bi bi-shield-lock me-2"></i>Admin Panel';

        adminLinkItem.appendChild(adminLink);

        // Find the divider's parent <li> and insert before it
        const dividerHr = dropdownMenu.querySelector('hr.dropdown-divider');
        if (dividerHr && dividerHr.parentElement && dividerHr.parentElement.tagName === 'LI') {
            dropdownMenu.insertBefore(adminLinkItem, dividerHr.parentElement);
        } else {
            // If no divider found, insert before logout button
            const logoutBtn = dropdownMenu.querySelector('#navLogoutBtn');
            if (logoutBtn && logoutBtn.closest('li')) {
                dropdownMenu.insertBefore(adminLinkItem, logoutBtn.closest('li'));
            } else {
                dropdownMenu.appendChild(adminLinkItem);
            }
        }

        console.log('✓ Admin Panel link added to navbar');
    }

    // Run when DOM is ready — auth refresh happens inside addAdminLinkToNavbar
    async function runAfterDomAndAuth() {
        // Wait for DOM ready if not already
        if (document.readyState === 'loading') {
            await new Promise(resolve => document.addEventListener('DOMContentLoaded', resolve));
        }

        // Now attempt to add admin link (will refresh token internally)
        await addAdminLinkToNavbar();
    }

    // Start the process (no setTimeout hacks)
    runAfterDomAndAuth().catch(e => console.error('[AdminNav] Failed to initialize:', e));

    // Also run when auth state changes (after login)
    window.addEventListener('auth-state-changed', addAdminLinkToNavbar);
})();
