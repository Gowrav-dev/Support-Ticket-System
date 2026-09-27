# AI-Powered IT Helpdesk & Support Ticket Management System

A beginner-friendly, clean full-stack application built with **Core Java (Java 17)**, **Java's built-in `HttpServer`**, **MySQL & JDBC** (with seamless embedded **H2 fallback**), **Groq LLM API**, and **Vanilla HTML5/CSS3/JavaScript**.

Specifically designed for IT placement interviews: transparent 4-tier architecture, no heavy frameworks, and clean code that is easy to understand, demonstrate, and explain.

---

## 🌟 Key Features

1. **Role-Based Portals & Two-Way Access Control**:
   - **User (Employee / Student)**: Submit IT support tickets, receive instant AI diagnostics, and track ticket status and assigned support staff. Blocked from accessing the Support Staff Portal.
   - **Support Staff (IT Helpdesk)**: View all tickets across the company, pick and assign tickets to themselves, update ticket statuses (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`), and filter by assignment status. Blocked from accessing the User Portal.
2. **Strict Mutual Page Isolation**:
   - Client-side gatekeepers in `<head>` immediately redirect unauthorized roles (`USER` redirected away from `/support.html`, and `SUPPORT_STAFF` redirected away from `/index.html`).
   - Server-side REST endpoints return `HTTP 403 Forbidden` if a user attempts to assign tickets or if staff attempts to submit user tickets.
2. **Multiple Support Staff Support**:
   - Multiple support staff members can register, log in, and collaborate concurrently.
   - 1-click demo switcher between Support Staff 1 (**Alex Rivera**) and Support Staff 2 (**Sarah Chen**).
3. **Ticket Assignment (`assigned_to`)**:
   - Identifies the specific support staff employee handling each ticket.
   - Support staff can click **"✋ Pick & Assign to Me"**, which updates `assigned_to` and automatically sets status to `IN_PROGRESS`.
   - Users can clearly see which support staff member is actively handling their issue.
4. **Instant AI Solution & Interactive Feedback Deflection**:
   - Evaluates issue title & description and provides step-by-step troubleshooting steps.
   - Presents the user with two interactive response buttons directly under the solution:
     - **`[ ✓ This solved my problem ]`**: Marks ticket as `RESOLVED`. AI deflection successful — no support staff intervention needed.
     - **`[ ✗ Problem still exists ]`**: Escalates ticket to `OPEN` and automatically routes it into the Support Staff dashboard for an IT specialist to pick and handle.
5. **Account Creation & Login**:
   - Dedicated Registration (`/register.html`) with role choice (`USER` or `SUPPORT_STAFF`).
   - Dedicated Login (`/login.html`) with password verification using **BCrypt** hashing.
   - 1-Click quick demo login buttons for rapid interview demonstrations.

---

## 🏗️ 4-Tier Architecture

```
Frontend (HTML5, CSS3, Vanilla JS)
       │
       ▼  (HTTP REST requests with JSON & Bearer Token)
Java HTTP Server (com.sun.net.httpserver.HttpServer)
       │
       ▼
Controllers (AuthController, TicketController, StaticFileController)
       │
       ▼
Services & DAOs (AuthService, AiService, TicketDao, UserDao)
       │
       ├──► Groq LLM API (llama-3.3-70b-versatile via Java 11 HttpClient)
       │
       ▼
Database (MySQL with embedded H2 fallback: users, tickets)
```

---

## 📁 Clean Project Structure

```
src/main/java/com/helpdesk/
├── Main.java                        # Starts Java HttpServer on port 8080
├── config/
│   └── Database.java                # JDBC connection manager & automatic schema init
├── model/
│   ├── User.java                    # User model (id, username, passwordHash, fullName, role)
│   └── Ticket.java                  # Ticket model (id, userName, title, description, category,
│                                    #               priority, aiSummary, aiSolution, status, assignedTo)
├── dao/
│   ├── UserDao.java                 # SQL queries for users (PreparedStatement)
│   └── TicketDao.java               # SQL queries for tickets (create, find, assign, updateStatus)
├── service/
│   ├── AuthService.java             # Register & login validation logic
│   └── AiService.java               # Groq LLM API integration & fallback diagnosis
├── controller/
│   ├── AuthController.java          # /api/auth/register, /api/auth/login, /api/auth/me, /api/auth/logout
│   ├── TicketController.java        # /api/tickets, /api/tickets/{id}, /api/tickets/{id}/assign, /status
│   └── StaticFileController.java    # Serves static web assets (HTML, CSS, JS)
└── util/
    ├── PasswordUtil.java            # BCrypt password hashing & verification
    ├── SessionManager.java          # In-memory token-to-user session map
    └── JsonUtil.java                # Gson JSON serializer & HTTP helper
```

---

## 🗄️ Database Tables

### 1. `users` Table
```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER', -- 'USER' or 'SUPPORT_STAFF'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2. `tickets` Table
```sql
CREATE TABLE tickets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) DEFAULT 'OTHER',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    ai_summary TEXT,
    ai_solution TEXT,
    status VARCHAR(20) DEFAULT 'OPEN',
    assigned_to VARCHAR(100) DEFAULT NULL, -- Support staff employee handling this ticket
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🚀 How to Run the Application

### 1. Run Unit Tests:
```powershell
.\mvn.cmd clean test
```

### 2. Start Application Server:
```powershell
.\mvn.cmd exec:java
```

### 3. Open in Browser:
- **User Portal**: [http://localhost:8080/](http://localhost:8080/)
- **Support Staff Portal**: [http://localhost:8080/support.html](http://localhost:8080/support.html)
- **Login Page**: [http://localhost:8080/login.html](http://localhost:8080/login.html)
- **Registration Page**: [http://localhost:8080/register.html](http://localhost:8080/register.html)

### Default Demo Accounts (Password: `Password@123`):
| Role | Username | Full Name | Purpose |
|------|----------|-----------|---------|
| Regular User | `john_doe` | John Doe | Submits tickets and views their status |
| Support Staff 1 | `alex_support` | Alex Rivera | IT Staff member who picks and handles tickets |
| Support Staff 2 | `sarah_support` | Sarah Chen | Demonstrates multiple concurrent support staff |

---

## 🔄 End-to-End Workflow (For Placement Interview Explanation)

1. **User Submits Ticket**: A user logs in as `john_doe` and submits a ticket (e.g. "VPN connection failed").
2. **Groq AI Analysis**: Core Java's `HttpClient` calls the Groq LLM API (`llama-3.3-70b-versatile`) to classify the ticket (`NETWORK`, `HIGH`) and generate step-by-step troubleshooting instructions.
3. **Saved with `assigned_to = NULL`**: The ticket is saved to the database with status `OPEN` and `assigned_to` initially empty.
4. **Support Staff Views Tickets**: Support staff member `alex_support` logs in and sees the unassigned ticket in the Support Staff Portal (`/support.html`).
5. **Staff Picks Ticket**: Alex clicks **"✋ Pick & Assign to Me"**. The backend updates `assigned_to = 'Alex Rivera'` and changes the status to `IN_PROGRESS`.
6. **User Visibility**: When John opens his User Portal, he immediately sees: `Assigned Support Staff: Alex Rivera (In Progress)`.
7. **Resolution**: Alex troubleshoots the issue and marks the status as `RESOLVED`.
