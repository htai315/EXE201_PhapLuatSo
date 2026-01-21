/**
 * Chat State Management Module
 * Centralized state for chat application.
 * 
 * Extracted from inline JS in legal-chat.html for better maintainability.
 * Single source of truth for chat UI state.
 */

// Chat state object - single source of truth
export const ChatState = {
    // Current active session ID (null = new chat)
    currentSessionId: null,
    
    // Pagination state for session list
    currentPage: 0,
    hasMoreSessions: false,
    isLoadingSessions: false,
    
    // Search state
    searchQuery: '',
    searchDebounceTimer: null,
    
    // UI state flags
    isSendingMessage: false,
    
    // Credits counter reference
    creditsCounter: null,
    
    // Auth token (loaded on init)
    // token snapshot deprecated - TokenManager is authoritative
    // token: null
};

/**
 * Reset state for new chat session
 */
export function resetChatState() {
    ChatState.currentSessionId = null;
}

/**
 * Set current session
 * @param {number|null} sessionId - Session ID or null for new chat
 */
export function setCurrentSession(sessionId) {
    ChatState.currentSessionId = sessionId;
}

/**
 * Update search state with debounce management
 * @param {string} query - Search query
 */
export function setSearchQuery(query) {
    ChatState.searchQuery = query;
    ChatState.currentPage = 0; // Reset pagination on new search
}

/**
 * Increment page for pagination
 */
export function nextPage() {
    ChatState.currentPage++;
}

/**
 * Reset pagination
 */
export function resetPagination() {
    ChatState.currentPage = 0;
}

/**
 * Set loading state for sessions
 * @param {boolean} isLoading
 */
export function setLoadingSessions(isLoading) {
    ChatState.isLoadingSessions = isLoading;
}

/**
 * Set sending message state (for UI feedback)
 * @param {boolean} isSending
 */
export function setSendingMessage(isSending) {
    ChatState.isSendingMessage = isSending;
}

/**
 * Initialize state with auth token
 * @param {string} token - Auth token from AUTH module
 */
export function initState(token) {
    // Deprecated: do not store token snapshot here. TokenManager is SSoT.
    // Left in place to avoid breaking callers that pass token; no-op.
}

/**
 * Clear search debounce timer
 */
export function clearSearchDebounce() {
    if (ChatState.searchDebounceTimer) {
        clearTimeout(ChatState.searchDebounceTimer);
        ChatState.searchDebounceTimer = null;
    }
}

/**
 * Set search debounce timer
 * @param {number} timerId - Timer ID from setTimeout
 */
export function setSearchDebounceTimer(timerId) {
    ChatState.searchDebounceTimer = timerId;
}
