/**
 * Admin Navigation Link
 * Automatically adds "Admin Panel" link to navbar for users with ADMIN role
 */

(function () {
    'use strict';

    // Check if user is admin and add admin link to navbar
    async function addAdminLinkToNavbar() {
        try {
            // Wait for TokenManager to be available
            if (!window.TokenManager) {
                console.log('[AdminNav] TokenManager not available');
                return;
            }

            // Only check if already authenticated - DO NOT call refresh here
            // script.js already handles rehydration, calling refresh again causes token reuse
            if (!window.TokenManager.isAuthenticated()) {
                console.log('[AdminNav] Not authenticated');
                return;
            }

            // Get user info to check role using API_CLIENT
            const user = await window.API_CLIENT?.get('/api/auth/me');
            if (!user) return;

            // Check if user has ADMIN role (handle both 'ADMIN' and 'ROLE_ADMIN')
            const isAdmin = user.role === 'ADMIN' || user.role === 'ROLE_ADMIN' ||
                (user.roles && (user.roles.includes('ADMIN') || user.roles.includes('ROLE_ADMIN')));

            if (isAdmin) {
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
            console.log('Dropdown menu not found, retrying...');
            setTimeout(insertAdminLink, 500);
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

    // Run when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            // Delay to ensure TokenManager and API_CLIENT are ready
            setTimeout(addAdminLinkToNavbar, 500);
        });
    } else {
        // Delay to ensure TokenManager and API_CLIENT are ready
        setTimeout(addAdminLinkToNavbar, 500);
    }

    // Also run when auth state changes (after login)
    window.addEventListener('auth-state-changed', addAdminLinkToNavbar);
})();
