package com.htai.exe201phapluatso.quiz.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_attempt_answers")
public class QuizAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private QuizAttempt attempt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuizQuestion question;

    @Column(name = "selected_option_key", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String selectedOptionKey;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    public Long getId() {
        return id;
    }

    public QuizAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(QuizAttempt attempt) {
        this.attempt = attempt;
    }

    public QuizQuestion getQuestion() {
        return question;
    }

    public void setQuestion(QuizQuestion question) {
        this.question = question;
    }

    public String getSelectedOptionKey() {
        return selectedOptionKey;
    }

    public void setSelectedOptionKey(String selectedOptionKey) {
        this.selectedOptionKey = selectedOptionKey;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}


