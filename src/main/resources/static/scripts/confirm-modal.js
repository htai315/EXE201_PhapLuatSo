/**
 * Confirm Modal System
 * Beautiful confirmation dialogs to replace browser confirm()
 */

class ConfirmModal {
    constructor() {
        this.modal = null;
        this.resolveCallback = null;
        this.init();
    }

    init() {
        // Create modal HTML if it doesn't exist
        if (!document.getElementById('confirmModal')) {
            const modalHTML = `
                <div class="modal fade" id="confirmModal" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content confirm-modal-content">
                            <div class="modal-body">
                                <div class="confirm-modal-icon" id="confirmModalIcon">
                                    <i class="bi bi-exclamation-triangle-fill"></i>
                                </div>
                                <h5 class="confirm-modal-title" id="confirmModalTitle">Xác nhận</h5>
                                <p class="confirm-modal-message" id="confirmModalMessage">Bạn có chắc chắn muốn thực hiện hành động này?</p>
                                <div class="confirm-modal-actions">
                                    <button type="button" class="btn btn-secondary" id="confirmModalCancel">
                                        <i class="bi bi-x-circle me-2"></i>Hủy
                                    </button>
                                    <button type="button" class="btn btn-danger" id="confirmModalConfirm">
                                        <i class="bi bi-check-circle me-2"></i>Xác nhận
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            document.body.insertAdjacentHTML('beforeend', modalHTML);
        }

        this.modal = new bootstrap.Modal(document.getElementById('confirmModal'));
        
        // Setup event listeners
        document.getElementById('confirmModalCancel').addEventListener('click', () => {
            this.hide(false);
        });

        document.getElementById('confirmModalConfirm').addEventListener('click', () => {
            this.hide(true);
        });

        // Handle backdrop click
        document.getElementById('confirmModal').addEventListener('hidden.bs.modal', () => {
            if (this.resolveCallback) {
                this.resolveCallback(false);
                this.resolveCallback = null;
            }
        });
    }

    /**
     * Show confirmation modal
     * @param {Object} options - Configuration options
     * @param {string} options.title - Modal title
     * @param {string} options.message - Modal message
     * @param {string} options.confirmText - Confirm button text
     * @param {string} options.cancelText - Cancel button text
     * @param {string} options.type - Modal type: 'danger', 'warning', 'info'
     * @returns {Promise<boolean>} - Returns true if confirmed, false if cancelled
     */
    show(options = {}) {
        const {
            title = 'Xác nhận',
            message = 'Bạn có chắc chắn muốn thực hiện hành động này?',
            confirmText = 'Xác nhận',
            cancelText = 'Hủy',
            type = 'danger'
        } = options;

        // Update modal content
        document.getElementById('confirmModalTitle').textContent = title;
        document.getElementById('confirmModalMessage').textContent = message;
        document.getElementById('confirmModalConfirm').innerHTML = `<i class="bi bi-check-circle me-2"></i>${confirmText}`;
        document.getElementById('confirmModalCancel').innerHTML = `<i class="bi bi-x-circle me-2"></i>${cancelText}`;

        // Update icon and button style based on type
        const iconElement = document.getElementById('confirmModalIcon');
        const confirmButton = document.getElementById('confirmModalConfirm');
        
        iconElement.className = 'confirm-modal-icon';
        confirmButton.className = 'btn';
        
        switch(type) {
            case 'danger':
                iconElement.classList.add('confirm-modal-icon-danger');
                iconElement.innerHTML = '<i class="bi bi-exclamation-triangle-fill"></i>';
                confirmButton.classList.add('btn-danger');
                break;
            case 'warning':
                iconElement.classList.add('confirm-modal-icon-warning');
                iconElement.innerHTML = '<i class="bi bi-exclamation-circle-fill"></i>';
                confirmButton.classList.add('btn-warning');
                break;
            case 'info':
                iconElement.classList.add('confirm-modal-icon-info');
                iconElement.innerHTML = '<i class="bi bi-info-circle-fill"></i>';
                confirmButton.classList.add('btn-primary');
                break;
            default:
                iconElement.classList.add('confirm-modal-icon-danger');
                iconElement.innerHTML = '<i class="bi bi-exclamation-triangle-fill"></i>';
                confirmButton.classList.add('btn-danger');
        }

        // Show modal
        this.modal.show();

        // Return promise
        return new Promise((resolve) => {
            this.resolveCallback = resolve;
        });
    }

    hide(result) {
        this.modal.hide();
        if (this.resolveCallback) {
            this.resolveCallback(result);
            this.resolveCallback = null;
        }
    }

    // Convenience methods
    danger(message, title = 'Xác nhận xóa') {
        return this.show({
            title,
            message,
            type: 'danger',
            confirmText: 'Xóa',
            cancelText: 'Hủy'
        });
    }

    warning(message, title = 'Cảnh báo') {
        return this.show({
            title,
            message,
            type: 'warning',
            confirmText: 'Tiếp tục',
            cancelText: 'Hủy'
        });
    }

    info(message, title = 'Thông báo') {
        return this.show({
            title,
            message,
            type: 'info',
            confirmText: 'OK',
            cancelText: 'Hủy'
        });
    }
}

// Create global instance
window.ConfirmModal = new ConfirmModal();

// Also create a global confirmAction function for easy use
window.confirmAction = async (message, title = 'Xác nhận') => {
    return await window.ConfirmModal.show({ message, title });
};

// Shorthand for delete confirmation
window.confirmDelete = async (itemName = 'mục này') => {
    return await window.ConfirmModal.danger(
        `Bạn có chắc chắn muốn xóa ${itemName}? Hành động này không thể hoàn tác.`,
        'Xác nhận xóa'
    );
};
