package com.helpdesk;

import com.helpdesk.config.Database;
import com.helpdesk.dao.TicketDao;
import com.helpdesk.dao.UserDao;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.service.AiService;
import com.helpdesk.service.AuthService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TicketTest {

    private static TicketDao ticketDao;
    private static UserDao userDao;
    private static AuthService authService;
    private static AiService aiService;

    @BeforeAll
    public static void setup() {
        Database.setTestUrl("jdbc:h2:mem:test_simple;MODE=MySQL;DB_CLOSE_DELAY=-1");
        Database.initDatabase();
        ticketDao = new TicketDao();
        userDao = new UserDao();
        authService = new AuthService();
        aiService = new AiService();
    }

    @Test
    public void testAuthAndSupportStaffFlow() throws Exception {
        // 1. Test Seeded demo users
        User alex = userDao.findByUsername("alex_support");
        assertNotNull(alex);
        assertEquals("SUPPORT_STAFF", alex.getRole());

        User sarah = userDao.findByUsername("sarah_support");
        assertNotNull(sarah);
        assertEquals("SUPPORT_STAFF", sarah.getRole());

        // 2. Test Register new user
        User newUser = authService.register("emily_test", "Password@123", "Emily Blunt", "USER");
        assertNotNull(newUser);
        assertEquals("emily_test", newUser.getUsername());

        // 3. Test Login
        AuthService.AuthResult loginResult = authService.login("alex_support", "Password@123");
        assertNotNull(loginResult.token);
        assertEquals("Alex Rivera", loginResult.user.getFullName());

        // 4. Test AI Analysis
        AiService.AiResult ai = aiService.analyzeTicket("VPN issue", "Cannot connect to company VPN.");
        assertNotNull(ai);
        assertNotNull(ai.category);
        assertNotNull(ai.suggestedSolution);

        // 5. Test Create Ticket by User (Initial state: PENDING_USER)
        Ticket ticket = new Ticket();
        ticket.setUserName(newUser.getFullName());
        ticket.setTitle("VPN issue");
        ticket.setDescription("Cannot connect to company VPN.");
        ticket.setCategory(ai.category);
        ticket.setPriority(ai.priority);
        ticket.setAiSummary(ai.summary);
        ticket.setAiSolution(ai.suggestedSolution);
        ticket.setStatus("PENDING_USER");
        ticket.setAssignedTo(null);

        Ticket created = ticketDao.createTicket(ticket);
        assertTrue(created.getId() > 0);
        assertEquals("PENDING_USER", created.getStatus());
        assertNull(created.getAssignedTo());

        // 6. User clicks "[ ✗ Problem still exists ]" -> status transitions to OPEN
        ticketDao.updateStatus(created.getId(), "OPEN");
        Ticket escalatedTicket = ticketDao.findById(created.getId());
        assertEquals("OPEN", escalatedTicket.getStatus());

        // 7. Support Staff (Alex) picks & handles the ticket
        boolean assigned = ticketDao.assignTicket(created.getId(), "Alex Rivera");
        assertTrue(assigned);

        Ticket updatedTicket = ticketDao.findById(created.getId());
        assertEquals("Alex Rivera", updatedTicket.getAssignedTo());
        assertEquals("IN_PROGRESS", updatedTicket.getStatus());

        // 8. Support Staff updates status to RESOLVED
        ticketDao.updateStatus(created.getId(), "RESOLVED");
        Ticket resolvedTicket = ticketDao.findById(created.getId());
        assertEquals("RESOLVED", resolvedTicket.getStatus());

        // 9. User queries their submitted tickets
        List<Ticket> userTickets = ticketDao.findByUserName(newUser.getFullName());
        assertFalse(userTickets.isEmpty());
        assertEquals("Alex Rivera", userTickets.get(0).getAssignedTo());

        // 10. Test AI Deflection: User clicks "[ ✓ This solved my problem ]"
        Ticket ticket2 = new Ticket();
        ticket2.setUserName(newUser.getFullName());
        ticket2.setTitle("How to reset password");
        ticket2.setDescription("Forgot my login password");
        ticket2.setStatus("PENDING_USER");
        Ticket created2 = ticketDao.createTicket(ticket2);

        // User clicks solved
        ticketDao.updateStatus(created2.getId(), "RESOLVED");
        Ticket solvedTicket = ticketDao.findById(created2.getId());
        assertEquals("RESOLVED", solvedTicket.getStatus());
        assertNull(solvedTicket.getAssignedTo()); // No staff was needed!
    }
}
