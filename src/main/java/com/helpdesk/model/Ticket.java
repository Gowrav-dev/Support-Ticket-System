package com.helpdesk.model;

import java.sql.Timestamp;

/**
 * Model class representing a Ticket, its AI diagnostic results, and assigned support staff.
 */
public class Ticket {
    private int id;
    private String userName;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String aiSummary;
    private String aiSolution;
    private String status;
    private String assignedTo; // Identifies the Support Staff employee handling the ticket
    private Timestamp createdAt;

    public Ticket() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getAiSolution() { return aiSolution; }
    public void setAiSolution(String aiSolution) { this.aiSolution = aiSolution; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
