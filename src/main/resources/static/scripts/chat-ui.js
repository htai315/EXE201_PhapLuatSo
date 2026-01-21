/**
 * Chat UI Module
 * Handles all DOM manipulations and UI rendering for chat.
 * 
 * Extracted from inline JS in legal-chat.html for better maintainability.
 * Pure rendering functions - no state management or API calls.
 */

import { ChatState } from './chat-state.js';

/**
 * DOM element references (cached after init)
 */
let elements = {
    chatMessages: null,
    questionInput: null,
    sendBtn: null,
    searchInput: null,
    clearSearch: null,
    recentChats: null,
    chatSidebar: null,
    sidebarOverlay: null,
    sidebarToggle: null
};

/**
 * Initialize DOM element references
 */
export function initElements() {
    elements.chatMessages = document.getElementById('chatMessages');
    elements.questionInput = document.getElementById('questionInput');
    elements.sendBtn = document.getElementById('sendBtn');
    elements.searchInput = document.getElementById('searchInput');
    elements.clearSearch = document.getElementById('clearSearch');
    elements.recentChats = document.getElementById('recentChats');
    elements.chatSidebar = document.getElementById('chatSidebar');
    elements.sidebarOverlay = document.getElementById('sidebarOverlay');
    elements.sidebarToggle = document.getElementById('sidebarToggle');
}

/**
 * Get DOM elements
 * @returns {Object} DOM element references
 */
export function getElements() {
    return elements;
}

/**
 * Escape HTML to prevent XSS
 * @param {string} text - Text to escape
 * @returns {string} Escaped HTML
 */
export function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Add a message to the chat display
 * @param {string} type - 'user' or 'bot'/'assistant'
 * @param {string} content - Message content
 * @param {Array|null} citations - Optional citations array
 */
export function addMessage(type, content, citations = null) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.textContent = type === 'user' ? '👤' : '🤖';

    const contentWrapper = document.createElement('div');

    const label = document.createElement('div');
    label.className = 'message-label';
    label.textContent = type === 'user' ? 'Bạn' : 'Trợ lý AI';
    contentWrapper.appendChild(label);

    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';

    if (type === 'bot' || type === 'assistant') {
        const answerText = content.replace(/\n/g, '<br>');
        contentDiv.innerHTML = `<p>${answerText}</p>`;

        // Add citations if available
        if (citations && Array.isArray(citations) && citations.length > 0) {
            const citationsDiv = document.createElement('div');
            citationsDiv.className = 'citations';
            citationsDiv.innerHTML = `
                <div class="citations-title">📚 Nguồn trích dẫn</div>
                ${citations.map(c => `
                    <div class="citation-item">
                        <div><strong>${escapeHtml(c.documentName || 'Văn bản pháp luật')}</strong></div>
                        <div class="citation-source">Điều ${c.articleNumber || 'N/A'}${c.articleTitle ? ' - ' + escapeHtml(c.articleTitle) : ''}</div>
                    </div>
                `).join('')}
            `;
            contentDiv.appendChild(citationsDiv);
        }
    } else {
        contentDiv.textContent = content;
    }

    contentWrapper.appendChild(contentDiv);
    messageDiv.appendChild(avatar);
    messageDiv.appendChild(contentWrapper);
    elements.chatMessages.appendChild(messageDiv);

    // Scroll to bottom
    elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
}

/**
 * Add typing indicator to chat
 * @returns {string} ID of the typing indicator element
 */
export function addTypingIndicator() {
    const id = 'typing-' + Date.now();
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message bot';
    messageDiv.id = id;

    messageDiv.innerHTML = `
        <div class="message-avatar">🤖</div>
        <div>
            <div class="message-label">Trợ lý AI</div>
            <div class="typing-indicator show">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
            </div>
        </div>
    `;

    elements.chatMessages.appendChild(messageDiv);
    elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;

    return id;
}

/**
 * Remove typing indicator
 * @param {string} id - Typing indicator element ID
 */
export function removeTypingIndicator(id) {
    const element = document.getElementById(id);
    if (element) element.remove();
}

/**
 * Set input disabled state
 * @param {boolean} disabled
 */
export function setInputDisabled(disabled) {
    elements.questionInput.disabled = disabled;
    elements.sendBtn.disabled = disabled;
}

/**
 * Focus on input
 */
export function focusInput() {
    elements.questionInput.focus();
}

/**
 * Get input value
 * @returns {string}
 */
export function getInputValue() {
    return elements.questionInput.value.trim();
}

/**
 * Clear input value
 */
export function clearInput() {
    elements.questionInput.value = '';
}

/**
 * Set input value
 * @param {string} value
 */
export function setInputValue(value) {
    elements.questionInput.value = value;
}

/**
 * Group sessions by date
 * @param {Array} sessions - Sessions array
 * @returns {Object} Grouped sessions
 */
export function groupSessionsByDate(sessions) {
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    const groups = {
        'Hôm nay': [],
        'Hôm qua': [],
        'Tuần này': [],
        'Cũ hơn': []
    };

    sessions.forEach(session => {
        const sessionDate = new Date(session.updatedAt);
        const sessionDay = new Date(sessionDate.getFullYear(), sessionDate.getMonth(), sessionDate.getDate());

        if (sessionDay.getTime() === today.getTime()) {
            groups['Hôm nay'].push(session);
        } else if (sessionDay.getTime() === yesterday.getTime()) {
            groups['Hôm qua'].push(session);
        } else if (sessionDay >= new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000)) {
            groups['Tuần này'].push(session);
        } else {
            groups['Cũ hơn'].push(session);
        }
    });

    // Remove empty groups
    Object.keys(groups).forEach(key => {
        if (groups[key].length === 0) {
            delete groups[key];
        }
    });

    return groups;
}

/**
 * Render chat sessions list
 * @param {Array} sessions - Sessions to render
 * @param {boolean} hasMore - Whether there are more sessions to load
 */
export function renderChatSessions(sessions, hasMore) {
    if (sessions.length === 0) {
        showEmptyState(ChatState.searchQuery ? 'Không tìm thấy kết quả' : 'Chưa có cuộc trò chuyện nào');
        return;
    }

    const groupedSessions = groupSessionsByDate(sessions);

    let html = '';
    for (const [label, sessionList] of Object.entries(groupedSessions)) {
        html += `<h3>${label}</h3>`;
        sessionList.forEach(session => {
            const isActive = session.id === ChatState.currentSessionId ? 'active' : '';
            html += createSessionItemHtml(session, isActive);
        });
    }

    // Add "Load More" button if there are more sessions
    if (hasMore) {
        html += `
            <button class="load-more-btn" onclick="window.ChatApp.loadMoreSessions()">
                <i class="bi bi-arrow-down-circle"></i>
                Xem thêm
            </button>
        `;
    }

    elements.recentChats.innerHTML = html;
}

/**
 * Append sessions to existing list (for pagination)
 * @param {Array} sessions
 * @param {boolean} hasMore
 */
export function appendChatSessions(sessions, hasMore) {
    // Remove existing "Load More" button
    const existingBtn = elements.recentChats.querySelector('.load-more-btn');
    if (existingBtn) {
        existingBtn.remove();
    }

    if (sessions.length === 0) {
        return;
    }

    const groupedSessions = groupSessionsByDate(sessions);

    let html = '';
    for (const [label, sessionList] of Object.entries(groupedSessions)) {
        // Check if group already exists
        const existingGroup = Array.from(elements.recentChats.querySelectorAll('h3'))
            .find(h3 => h3.textContent === label);

        if (!existingGroup) {
            html += `<h3>${label}</h3>`;
        }

        sessionList.forEach(session => {
            const isActive = session.id === ChatState.currentSessionId ? 'active' : '';
            html += createSessionItemHtml(session, isActive);
        });
    }

    // Add "Load More" button if there are more sessions
    if (hasMore) {
        html += `
            <button class="load-more-btn" onclick="window.ChatApp.loadMoreSessions()">
                <i class="bi bi-arrow-down-circle"></i>
                Xem thêm
            </button>
        `;
    }

    elements.recentChats.insertAdjacentHTML('beforeend', html);
}

/**
 * Create session item HTML
 * @param {Object} session
 * @param {string} activeClass
 * @returns {string}
 */
function createSessionItemHtml(session, activeClass) {
    return `
        <div class="chat-item ${activeClass}" data-session-id="${session.id}">
            <div class="chat-item-content" onclick="window.ChatApp.loadSession(${session.id}, this)">
                <div class="chat-item-icon">
                    <i class="bi bi-chat-text"></i>
                </div>
                <div class="chat-item-text">
                    <div class="chat-item-title">${escapeHtml(session.title)}</div>
                </div>
            </div>
            <button class="chat-item-delete" onclick="window.ChatApp.deleteSession(${session.id}, event)" title="Xóa cuộc trò chuyện">
                <i class="bi bi-trash3"></i>
            </button>
        </div>
    `;
}

/**
 * Show empty state in sessions list
 * @param {string} message
 */
export function showEmptyState(message) {
    elements.recentChats.innerHTML = `
        <div class="empty-state">
            <i class="bi bi-chat-dots"></i>
            <p>${message}</p>
        </div>
    `;
}

/**
 * Clear chat messages and show welcome message
 */
export function showWelcomeMessage() {
    elements.chatMessages.innerHTML = `
        <div class="message bot">
            <div class="message-avatar">🤖</div>
            <div>
                <div class="message-label">Trợ lý AI</div>
                <div class="message-content">
                    <p>Xin chào! Tôi là trợ lý AI chuyên về pháp luật Việt Nam. Bạn có thể hỏi tôi về các vấn đề pháp lý và tôi sẽ trả lời dựa trên các văn bản pháp luật đã được cập nhật.</p>
                </div>
            </div>
        </div>
    `;
}

/**
 * Clear all messages
 */
export function clearMessages() {
    elements.chatMessages.innerHTML = '';
}

/**
 * Update active state in sidebar
 * @param {number|null} sessionId - Active session ID
 */
export function updateActiveSession(sessionId) {
    document.querySelectorAll('.chat-item').forEach(item => {
        item.classList.remove('active');
        if (sessionId && item.dataset.sessionId === String(sessionId)) {
            item.classList.add('active');
        }
    });
}

/**
 * Toggle mobile sidebar
 */
export function toggleSidebar() {
    elements.chatSidebar.classList.toggle('show');
    elements.sidebarOverlay.classList.toggle('show');

    // Update toggle button icon
    const icon = elements.sidebarToggle.querySelector('i');
    if (elements.chatSidebar.classList.contains('show')) {
        icon.className = 'bi bi-x-lg';
    } else {
        icon.className = 'bi bi-clock-history';
    }
}

/**
 * Close mobile sidebar
 */
export function closeSidebar() {
    elements.chatSidebar.classList.remove('show');
    elements.sidebarOverlay.classList.remove('show');
    const icon = elements.sidebarToggle.querySelector('i');
    icon.className = 'bi bi-clock-history';
}

/**
 * Show/hide clear search button
 * @param {boolean} show
 */
export function toggleClearSearchButton(show) {
    if (show) {
        elements.clearSearch.classList.add('show');
    } else {
        elements.clearSearch.classList.remove('show');
    }
}

/**
 * Clear search input
 */
export function clearSearchInput() {
    elements.searchInput.value = '';
    elements.searchInput.focus();
}
