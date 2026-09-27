# 🎓 Placement Technical Interview Guide
## AI-Powered IT Helpdesk & Support Ticket Management System

This guide is written in clear, beginner-friendly language so you can study, understand, and confidently explain every aspect of this project in a placement interview within **1–2 hours**.

---

## 1. Top 11 Important Files to Understand

| # | File | Purpose (Simple Explanation) |
|---|---|---|
| 1 | **`Main.java`** | Application entry point. Initializes database tables & demo accounts, starts Java's built-in `HttpServer` on port 8080, and maps routes (`/api/auth`, `/api/tickets`, `/`). |
| 2 | **`config/Database.java`** | Connects to MySQL using JDBC (`DriverManager.getConnection`). Seamlessly falls back to embedded H2 if MySQL is not configured. Creates `users` and `tickets` tables and seeds demo accounts. |
| 3 | **`model/User.java`** | Model representing an employee or IT staff (`id`, `username`, `passwordHash`, `fullName`, `role`). |
| 4 | **`model/Ticket.java`** | Model representing a ticket (`id`, `userName`, `title`, `description`, `category`, `priority`, `aiSummary`, `aiSolution`, `status`, `assignedTo`). |
| 5 | **`dao/UserDao.java`** | Executes SQL queries to register users and find users by username using `PreparedStatement`. |
| 6 | **`dao/TicketDao.java`** | Executes SQL queries to create tickets, find tickets by user, list all tickets, assign tickets to support staff, and update status. |
| 7 | **`service/AuthService.java`** | Handles registration and login. Verifies credentials with BCrypt and creates active sessions. |
| 8 | **`service/AiService.java`** | Calls the Groq LLM API using Java 11 `HttpClient` to classify issues and generate troubleshooting steps, with an intelligent local fallback. |
| 9 | **`controller/AuthController.java`** | Handles HTTP requests for `/api/auth/register`, `/api/auth/login`, `/api/auth/me`, and `/api/auth/logout`. |
| 10 | **`controller/TicketController.java`** | Handles HTTP requests for `/api/tickets` (create ticket, list tickets, view details, pick/assign ticket via `/assign`, update status via `/status`). |
| 11 | **`util/SessionManager.java`** | Lightweight thread-safe session tracker mapping authentication tokens to logged-in `User` objects. |

---

## 2. Complete Request Flow (Step-by-Step)

When an interviewer asks: *"Explain what happens from the moment a user creates a ticket to when a support staff member picks and resolves it"*, give this exact response:

```
Step 1: User Submits Ticket
  └─ Employee John Doe enters issue title ("VPN connection failing") and description.
  └─ JavaScript sends an HTTP POST request to /api/tickets with John's authentication token.

Step 2: AI Diagnosis via Groq API
  └─ TicketController invokes AiService.analyzeTicket(title, description).
  └─ Java 11 HttpClient calls Groq LLM (llama-3.3-70b-versatile).
  └─ Groq returns structured JSON:
       - Category: "NETWORK"
       - Priority: "HIGH"
       - Summary: "User experiencing VPN authentication failure."
       - Solution: "1. Reset network adapter. 2. Verify token credentials. 3. Reinstall VPN client."

Step 3: Saved to Database
  └─ TicketDao executes an INSERT query via JDBC PreparedStatement.
  └─ Ticket is saved with status = 'OPEN' and assigned_to = NULL (unassigned).

Step 4: Support Staff Reviews Tickets
  └─ Support Staff member Alex Rivera logs into /support.html.
  └─ The portal queries /api/tickets and displays all open tickets.
  └─ Ticket #1 shows as [Unassigned] with a "✋ Pick & Assign to Me" button.

Step 5: Support Staff Picks Ticket (assigned_to)
  └─ Alex clicks "Pick & Assign to Me".
  └─ Frontend calls POST /api/tickets/1/assign.
  └─ TicketDao executes:
       UPDATE tickets SET assigned_to = 'Alex Rivera', status = 'IN_PROGRESS' WHERE id = 1;
  └─ The ticket is now officially assigned to Alex Rivera!

Step 6: User Visibility & Resolution
  └─ In John Doe's User Portal, the ticket now shows: "Assigned Support Staff: 👤 Alex Rivera (In Progress)".
  └─ Once Alex resolves the issue, he changes the status to RESOLVED via the dropdown.
```

---

## 3. Common Interview Questions & Answers

### Q1: Why did you rename the Admin role to "Support Staff"?
> **Answer:**
> *"In a realistic enterprise IT helpdesk, tickets are not handled by system administrators, but by an **IT Support Staff team**. Multiple support staff members (e.g. Alex Rivera, Sarah Chen) work together to triage, pick, and resolve tickets. Changing the role to `SUPPORT_STAFF` accurately mirrors real-world ITIL and enterprise service desk operations."*

---

### Q2: How does the system handle multiple support staff members?
> **Answer:**
> *"1. Anyone can register as a `SUPPORT_STAFF` member through `/register.html`.
> 2. Each support staff member has their own authenticated account.
> 3. In the database, the `tickets` table has an `assigned_to VARCHAR(100)` column that records the specific support staff member currently handling that ticket.
> 4. In the Support Staff Portal (`/support.html`), staff can filter tickets by 'All', 'Assigned to Me', or 'Unassigned', and can pick unassigned tickets with a single click."*

---

### Q3: Why did you use Core Java and built-in HttpServer instead of Spring Boot?
> **Answer:**
> *"I chose Core Java and Java's built-in `HttpServer` (`com.sun.net.httpserver`) to understand and demonstrate how web servers, HTTP request routing, thread handling, and JDBC work at the fundamental level, without relying on framework magic or heavy abstractions."*

---

### Q4: How do you prevent SQL Injection?
> **Answer:**
> *"By exclusively using `PreparedStatement` with parameterized placeholders (`?`). The database pre-compiles the SQL command structure before user inputs are bound. All user inputs are treated strictly as string data literals and can never alter the SQL execution logic."*

---

### Q5: How does authentication and password security work?
> **Answer:**
> *"1. Passwords are never stored in plaintext. We hash them using the **BCrypt** hashing algorithm (`PasswordUtil.hash()`), which applies a cryptographically secure random salt and work factor.
> 2. During login, `BCrypt.checkpw()` safely verifies the password against the stored hash.
> 3. Upon successful login, the server generates a unique session token (`UUID`) managed by `SessionManager`."*

---

### Q6: How does the AI integration work, and what happens if the Groq API is offline?
> **Answer:**
> *"When a ticket is created, `AiService` sends the title and description to the Groq LLM API using Java 11's native `HttpClient`. We prompt the model to return structured JSON with category, priority, summary, and troubleshooting steps.
> If the API key is absent or network fails, `AiService` catches the exception and immediately invokes a fast rule-based local diagnosis fallback based on keyword heuristics. This guarantees 100% uptime and resilience."*
