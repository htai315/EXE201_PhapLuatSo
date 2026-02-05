/**
 * Chat Main Module - Entry Point
 * Orchestrates chat functionality by connecting state, API, and UI modules.
 * 
 * Extracted from inline JS in legal-chat.html for better maintainability.
 * This module handles initialization and event binding.
 */

import {
    ChatState,
    initState,
    setCurrentSession,
    resetChatState,
    setSearchQuery,
    nextPage,
    resetPagination,
    setLoadingSessions,
    setSendingMessage,
    clearSearchDebounce,
    setSearchDebounceTimer
} from './chat-state.js';

import {
    loadChatSessions as apiLoadSessions,
    loadSessionMessages as apiLoadMessages,
    sendMessage as apiSendMessage,
    deleteSession as apiDeleteSession
} from './chat-api.js';

import {
    initElements,
    getElements,
    addMessage,
    addTypingIndicator,
    removeTypingIndicator,
    setInputDisabled,
    focusInput,
    getInputValue,
    clearInput,
    setInputValue,
    renderChatSessions,
    appendChatSessions,
    showEmptyState,
    showWelcomeMessage,
    clearMessages,
    updateActiveSession,
    toggleSidebar,
    closeSidebar,
    toggleClearSearchButton,
    clearSearchInput,
    setupTextareaHandlers,
    autoResizeTextarea
} from './chat-ui.js';

// Import credits counter (loaded separately)
let CreditsCounter;

/**
 * Initialize chat application
 * Called after DOM is ready and user is authenticated
 */
export async function initChat() {
    // Check auth
    if (typeof AUTH === 'undefined' || !AUTH.isLoggedIn()) {
        console.warn('User not logged in, chat not initialized');
        return;
    }

    // initState is deprecated; TokenManager is authoritative

    // Initialize DOM elements
    initElements();

    // Initialize credits counter
    try {
        CreditsCounter = window.CreditsCounter;
        if (CreditsCounter) {
            ChatState.creditsCounter = new CreditsCounter();
            ChatState.creditsCounter.init('chatCreditsCounter', 'chat');
        }
    } catch (e) {
        console.warn('Credits counter not available:', e);
    }

    // Setup event listeners
    setupEventListeners();

    // Load initial sessions
    await loadChatSessions(true);

    console.log('Chat initialized successfully');
}

/**
 * Setup all event listeners
 */
function setupEventListeners() {
    const elements = getElements();

    // Mobile sidebar toggle
    if (elements.sidebarToggle) {
        elements.sidebarToggle.addEventListener('click', toggleSidebar);
    }

    if (elements.sidebarOverlay) {
        elements.sidebarOverlay.addEventListener('click', closeSidebar);
    }

    // Search with debounce
    elements.searchInput.addEventListener('input', handleSearchInput);
    elements.clearSearch.addEventListener('click', handleClearSearch);

    // Chat form submission
    const chatForm = document.getElementById('chatForm');
    if (chatForm) {
        chatForm.addEventListener('submit', handleFormSubmit);
    }

    // Setup textarea auto-resize and Enter key handling
    setupTextareaHandlers(() => {
        if (chatForm) chatForm.dispatchEvent(new Event('submit'));
    });
}

/**
 * Handle search input with debounce
 * @param {Event} e
 */
function handleSearchInput(e) {
    const value = e.target.value.trim();

    // Show/hide clear button
    toggleClearSearchButton(!!value);

    // Debounce search
    clearSearchDebounce();
    const timerId = setTimeout(() => {
        setSearchQuery(value);
        loadChatSessions(true);
    }, 300);
    setSearchDebounceTimer(timerId);
}

/**
 * Handle clear search button click
 */
function handleClearSearch() {
    clearSearchInput();
    setSearchQuery('');
    resetPagination();
    toggleClearSearchButton(false);
    loadChatSessions(true);
}

/**
 * Handle form submission
 * @param {Event} e
 */
async function handleFormSubmit(e) {
    e.preventDefault();

    const question = getInputValue();
    if (!question) return;

    // Prevent duplicate sends
    if (ChatState.isSendingMessage) return;
    setSendingMessage(true);

    // Add user message to UI
    addMessage('user', question);
    clearInput();

    // Show typing indicator
    const typingId = addTypingIndicator();

    // Disable input
    setInputDisabled(true);

    try {
        // Send to API
        const data = await apiSendMessage(question, ChatState.currentSessionId);

        // Update session if new
        if (!ChatState.currentSessionId) {
            setCurrentSession(data.sessionId);
            resetPagination();
            await loadChatSessions(true);
        }

        // Remove typing indicator
        removeTypingIndicator(typingId);

        // Add bot response with null-safety check
        if (data && data.assistantMessage && data.assistantMessage.content) {
            addMessage('bot', data.assistantMessage.content, data.assistantMessage.citations);
        } else {
            // Fallback for unexpected response structure
            const fallbackContent = data?.error || data?.message || 'Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.';
            addMessage('bot', fallbackContent);
            console.warn('Unexpected API response structure:', data);
        }

        // Refresh credits counter
        if (ChatState.creditsCounter) {
            ChatState.creditsCounter.refresh();
        }
    } catch (error) {
        removeTypingIndicator(typingId);
        addMessage('bot', error.message || 'Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.');
        console.error('Error:', error);
    } finally {
        setInputDisabled(false);
        setSendingMessage(false);
        focusInput();
    }
}

/**
 * Load chat sessions from API
 * @param {boolean} reset - Whether to reset pagination
 */
async function loadChatSessions(reset = false) {
    if (ChatState.isLoadingSessions) return;
    setLoadingSessions(true);

    try {
        const page = reset ? 0 : ChatState.currentPage;
        const data = await apiLoadSessions(page, 20, ChatState.searchQuery);

        if (reset) {
            renderChatSessions(data.sessions, data.hasMore);
        } else {
            appendChatSessions(data.sessions, data.hasMore);
        }

        ChatState.hasMoreSessions = data.hasMore;

    } catch (error) {
        console.error('Error loading sessions:', error);
        showEmptyState('Không thể tải lịch sử trò chuyện');
    } finally {
        setLoadingSessions(false);
    }
}

/**
 * Load more sessions (pagination)
 */
export async function loadMoreSessions() {
    nextPage();
    await loadChatSessions(false);
}

/**
 * Load a specific session
 * @param {number} sessionId
 * @param {HTMLElement} clickedElement
 */
export async function loadSession(sessionId, clickedElement) {
    try {
        const messages = await apiLoadMessages(sessionId);

        // Clear current messages
        clearMessages();

        // Set current session
        setCurrentSession(sessionId);

        // Render messages
        messages.forEach(msg => {
            addMessage(msg.role.toLowerCase(), msg.content, msg.citations);
        });

        // Update active state in sidebar
        updateActiveSession(sessionId);

        // Close sidebar on mobile
        if (window.innerWidth <= 768) {
            closeSidebar();
        }
    } catch (error) {
        console.error('Error loading session:', error);
        if (window.Toast) {
            window.Toast.error('Không thể tải cuộc trò chuyện');
        }
    }
}

/**
 * Delete a session
 * @param {number} sessionId
 * @param {Event} event
 */
export async function deleteSession(sessionId, event) {
    // Prevent triggering loadSession
    event.stopPropagation();

    if (!confirm('Bạn có chắc muốn xóa cuộc trò chuyện này?')) {
        return;
    }

    try {
        await apiDeleteSession(sessionId);

        // Remove from DOM
        const chatItem = document.querySelector(`.chat-item[data-session-id="${sessionId}"]`);
        if (chatItem) {
            chatItem.remove();
        }

        // If deleted current session, start new chat
        if (ChatState.currentSessionId === sessionId) {
            startNewChat();
        }

        if (window.Toast) {
            window.Toast.success('Đã xóa cuộc trò chuyện');
        }

        // Reload sessions to update groups
        resetPagination();
        await loadChatSessions(true);

    } catch (error) {
        console.error('Error deleting session:', error);
        if (window.Toast) {
            window.Toast.error('Không thể xóa cuộc trò chuyện');
        }
    }
}

/**
 * Start a new chat session
 */
export function startNewChat() {
    resetChatState();
    showWelcomeMessage();
    updateActiveSession(null);
    focusInput();
}

/**
 * Ask an example question (from quick actions)
 * @param {string} question
 */
export function askExample(question) {
    setInputValue(question);
    const chatForm = document.getElementById('chatForm');
    if (chatForm) {
        chatForm.dispatchEvent(new Event('submit'));
    }
}

// Export for global access (needed for onclick handlers in HTML)
window.ChatApp = {
    loadSession,
    deleteSession,
    loadMoreSessions,
    startNewChat,
    askExample
};

// Auto-initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    // Wait for auth-guard / rehydration to complete before initializing chat.
    // Use AUTH.guard when available (non-redirecting) or fallback to TokenManager refresh.
    (async () => {
        try {
            let ready = false;
            try {
                ready = await AppRuntime.authReady();
            } catch (e) {
                console.warn('[Chat] authReady rejected', e);
                ready = false;
            }

            if (ready && typeof AUTH !== 'undefined' && AUTH.isLoggedIn()) {
                initChat();
            }
        } catch (err) {
            console.warn('Chat init error', err);
        }
    })();
});
