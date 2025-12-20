package com.htai.exe201phapluatso.quiz.entity;

import com.htai.exe201phapluatso.auth.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_set_id")
    private QuizSet quizSet;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt = LocalDateTime.now();

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "score_percent", nullable = false)
    private int scorePercent; // 0-100

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public QuizSet getQuizSet() {
        return quizSet;
    }

    public void setQuizSet(QuizSet quizSet) {
        this.quizSet = quizSet;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getScorePercent() {
        return scorePercent;
    }

    public void setScorePercent(int scorePercent) {
        this.scorePercent = scorePercent;
    }
}


