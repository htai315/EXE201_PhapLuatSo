/**
 * Chat API Module
 * Handles all API calls for chat functionality.
 * 
 * Extracted from inline JS in legal-chat.html for better maintainability.
 * All fetch operations are centralized here.
 */

import { ChatState } from './chat-state.js';

/**
 * API endpoints configuration
 */
const API_ENDPOINTS = {
    SESSIONS: '/api/chat/sessions',
    SESSION_MESSAGES: (sessionId) => `/api/chat/sessions/${sessionId}/messages`,
    NEW_SESSION_MESSAGE: '/api/chat/sessions/messages',
    DELETE_SESSION: (sessionId) => `/api/chat/sessions/${sessionId}`
};

/**
 * Get authorization headers
 * @returns {Object} Headers object with Authorization
 */
function getAuthHeaders() {
    // Deprecated: ChatState.token snapshot removed — apiClient is the single auth gateway.
    return {
        'Content-Type': 'application/json'
    };
}

/**
 * Load chat sessions with optional pagination and search
 * @param {number} page - Page number (0-indexed)
 * @param {number} size - Page size
 * @param {string} search - Optional search query
 * @returns {Promise<Object>} Sessions response with pagination info
 */
export async function loadChatSessions(page = 0, size = 20, search = '') {
    const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString()
    });

    if (search) {
        params.append('search', search);
    }
    // Route through central api client which handles token refresh and retries
    const client = AppRuntime.getClient();
    if (!client) throw new Error('API client not available');
    return await AppRuntime.safe('ChatAPI:loadSessions', () => client.get(`${API_ENDPOINTS.SESSIONS}?${params}`));
}

/**
 * Load messages for a specific session
 * @param {number} sessionId - Session ID
 * @returns {Promise<Array>} Array of messages
 */
export async function loadSessionMessages(sessionId) {
    const client = AppRuntime.getClient();
    if (!client) throw new Error('API client not available');
    return await AppRuntime.safe('ChatAPI:loadMessages', () => client.get(API_ENDPOINTS.SESSION_MESSAGES(sessionId)));
}

/**
 * Send a message (either in new session or existing session)
 * @param {string} question - User's question
 * @param {number|null} sessionId - Session ID (null for new session)
 * @returns {Promise<Object>} Response with sessionId, userMessage, assistantMessage
 */
export async function sendMessage(question, sessionId = null) {
    const endpoint = sessionId
        ? API_ENDPOINTS.SESSION_MESSAGES(sessionId)
        : API_ENDPOINTS.NEW_SESSION_MESSAGE;

    try {
        const client = AppRuntime.getClient();
        if (!client) throw new Error('API client not available');
        return await AppRuntime.safe('ChatAPI:sendMessage', () => client.post(endpoint, { question }));
    } catch (error) {
        // Normalize error to match previous behavior (throw Error with message)
        if (error && typeof error === 'object' && error.error) {
            throw new Error(error.error || error.message || 'Có lỗi xảy ra');
        }
        if (error && error.message) {
            throw new Error(error.message);
        }
        throw new Error('Có lỗi xảy ra');
    }
}

/**
 * Delete a chat session
 * @param {number} sessionId - Session ID to delete
 * @returns {Promise<void>}
 */
export async function deleteSession(sessionId) {
    const client = AppRuntime.getClient();
    if (!client) throw new Error('API client not available');
    await AppRuntime.safe('ChatAPI:deleteSession', () => client.delete(API_ENDPOINTS.DELETE_SESSION(sessionId)));
}

export { API_ENDPOINTS };
