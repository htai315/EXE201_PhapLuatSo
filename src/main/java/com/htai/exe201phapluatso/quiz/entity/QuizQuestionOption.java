package com.htai.exe201phapluatso.quiz.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_question_options")
public class QuizQuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id")
    private QuizQuestion question;

    @Column(name="option_key", nullable=false, length=1, columnDefinition = "CHAR(1)")
    private String optionKey;

    @Column(name="option_text", nullable=false, length=1000)
    private String optionText;

    @Column(name="is_correct", nullable=false)
    private boolean isCorrect;

    // getters
    public Long getId() { return id; }
    public QuizQuestion getQuestion() { return question; }
    public String getOptionKey() { return optionKey; }
    public String getOptionText() { return optionText; }
    public boolean isCorrect() { return isCorrect; }

    // setters
    public void setQuestion(QuizQuestion question) { this.question = question; }
    public void setOptionKey(String optionKey) { this.optionKey = optionKey; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
}
