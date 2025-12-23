package com.htai.exe201phapluatso.auth.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "plans")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // FREE, STUDENT

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(name = "chat_credits", nullable = false)
    private int chatCredits = 0;

    @Column(name = "quiz_gen_credits", nullable = false)
    private int quizGenCredits = 0;

    @Column(name = "duration_months", nullable = false)
    private int durationMonths = 12;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getChatCredits() { return chatCredits; }
    public void setChatCredits(int chatCredits) { this.chatCredits = chatCredits; }

    public int getQuizGenCredits() { return quizGenCredits; }
    public void setQuizGenCredits(int quizGenCredits) { this.quizGenCredits = quizGenCredits; }

    public int getDurationMonths() { return durationMonths; }
    public void setDurationMonths(int durationMonths) { this.durationMonths = durationMonths; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
