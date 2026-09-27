package com.helpdesk.dao;

import com.helpdesk.config.Database;
import com.helpdesk.model.Ticket;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object: Handles all SQL queries for the tickets table,
 * including ticket creation, assignment to Support Staff, and status updates.
 */
public class TicketDao {

    public Ticket createTicket(Ticket ticket) throws SQLException {
        String sql = """
            INSERT INTO tickets (user_name, title, description, category, priority, ai_summary, ai_solution, status, assigned_to)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, ticket.getUserName());
            stmt.setString(2, ticket.getTitle());
            stmt.setString(3, ticket.getDescription());
            stmt.setString(4, ticket.getCategory() != null ? ticket.getCategory() : "OTHER");
            stmt.setString(5, ticket.getPriority() != null ? ticket.getPriority() : "MEDIUM");
            stmt.setString(6, ticket.getAiSummary());
            stmt.setString(7, ticket.getAiSolution());
            stmt.setString(8, ticket.getStatus() != null ? ticket.getStatus() : "OPEN");
            stmt.setString(9, ticket.getAssignedTo());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    ticket.setId(rs.getInt(1));
                }
            }
            return ticket;
        }
    }

    public Ticket findById(int id) throws SQLException {
        String sql = "SELECT * FROM tickets WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTicket(rs);
                }
            }
        }
        return null;
    }

    public List<Ticket> findAll() throws SQLException {
        String sql = "SELECT * FROM tickets ORDER BY id DESC";
        List<Ticket> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToTicket(rs));
            }
        }
        return list;
    }

    public List<Ticket> findByUserName(String userName) throws SQLException {
        String sql = "SELECT * FROM tickets WHERE user_name = ? ORDER BY id DESC";
        List<Ticket> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTicket(rs));
                }
            }
        }
        return list;
    }

    public boolean assignTicket(int ticketId, String supportStaffName) throws SQLException {
        String sql = "UPDATE tickets SET assigned_to = ?, status = CASE WHEN status = 'OPEN' THEN 'IN_PROGRESS' ELSE status END WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, supportStaffName);
            stmt.setInt(2, ticketId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(int ticketId, String newStatus) throws SQLException {
        String sql = "UPDATE tickets SET status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus.toUpperCase());
            stmt.setInt(2, ticketId);
            return stmt.executeUpdate() > 0;
        }
    }

    private Ticket mapRowToTicket(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.setId(rs.getInt("id"));
        t.setUserName(rs.getString("user_name"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setCategory(rs.getString("category"));
        t.setPriority(rs.getString("priority"));
        t.setAiSummary(rs.getString("ai_summary"));
        t.setAiSolution(rs.getString("ai_solution"));
        t.setStatus(rs.getString("status"));
        t.setAssignedTo(rs.getString("assigned_to"));
        t.setCreatedAt(rs.getTimestamp("created_at"));
        return t;
    }
}
