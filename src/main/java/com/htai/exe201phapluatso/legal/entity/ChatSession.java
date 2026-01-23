    package com.htai.exe201phapluatso.legal.entity;

    import com.htai.exe201phapluatso.auth.entity.User;
    import jakarta.persistence.*;
    import java.time.LocalDateTime;
    import java.util.ArrayList;
    import java.util.List;

    @Entity
    @Table(name = "chat_sessions")
    public class ChatSession {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @Column(nullable = false, length = 200)
        private String title;

        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt = LocalDateTime.now();

        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt = LocalDateTime.now();

        // Session billing fields
        @Column(name = "user_question_count", nullable = false)
        private Integer userQuestionCount = 0;

        @Column(name = "charge_state", nullable = false, length = 20)
        private String chargeState = "NOT_CHARGED";

        @Column(name = "charge_reservation_id")
        private Long chargeReservationId;

        @Column(nullable = false)
        @Version
        private Integer version = 0;

        @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<ChatMessage> messages = new ArrayList<>();

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        // Session billing getters and setters
        public Integer getUserQuestionCount() {
            return userQuestionCount;
        }

        public void setUserQuestionCount(Integer userQuestionCount) {
            this.userQuestionCount = userQuestionCount;
        }

        public String getChargeState() {
            return chargeState;
        }

        public void setChargeState(String chargeState) {
            this.chargeState = chargeState;
        }

        public Long getChargeReservationId() {
            return chargeReservationId;
        }

        public void setChargeReservationId(Long chargeReservationId) {
            this.chargeReservationId = chargeReservationId;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public List<ChatMessage> getMessages() {
            return messages;
        }

        public void setMessages(List<ChatMessage> messages) {
            this.messages = messages;
        }

        public void addMessage(ChatMessage message) {
            messages.add(message);
            message.setSession(this);
        }
    }
