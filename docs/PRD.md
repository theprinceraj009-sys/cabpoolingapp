# PRODUCT REQUIREMENTS DOCUMENT (PRD) – IMPROVED (ERP + MICROservices + PRODUCTION GRADE)

---

## 1. PRODUCT VISION & GOALS

**Primary Goal:**
The application aims to provide **cost savings, convenience, and safety** for campus users by enabling structured taxi pooling.

- Cost saving through shared rides
- Convenience via centralized discovery (no manual asking)
- Safety through verified campus-only users

**Core Concept:**
A **social + ride-sharing hybrid platform** where users post planned journeys and others connect to share rides.

**Problem Solved (vs traditional apps like Uber/Ola):**
Unlike platforms such as Uber/Ola, this system:
- Does NOT provide taxis
- Enables **peer-to-peer ride coordination**
- Focuses on **pre-planned ride sharing within trusted communities**

**Scope:**
- Phase 1: Single campus (Chandigarh University)
- Phase 2: Multi-campus
- Phase 3: City-level expansion

**USP (Unique Value):**
- Campus-restricted trust network
- Ride-sharing as a social feed
- Privacy-controlled identity sharing

**Product Nature:**
- Designed as a **production-level startup-ready system** (not just academic project)

---

## 2. USERS & ROLES

**User Types:**
- Student
- Faculty/Staff (tagged profiles)
- Admin

**Driver Accounts:**
- Not applicable (only passengers)

**Verification:**
- OTP-based email verification (college + optional personal email)

**Profile Data:**
- Name, UID, Department
- Role Tag (Student/Faculty)
- Social Links (optional)
- Contact info (hidden until connection)

**Privacy Model:**
- Limited profile visibility by default
- Full details unlocked after connection or premium

**Admin Capabilities:**
- Ban users
- View analytics
- Access flagged content only (NOT all chats)

---

## 3. RIDE MANAGEMENT SYSTEM

**Ride Posting Fields:**
- Source & destination
- Route description
- Date & time
- Total fare (manual)
- Seat availability
- Preferences/rules
- Proof upload (image/pdf/text)

**Ride Behavior:**
- Real-time seat updates
- Auto-close when full or start time reached
- Users can edit or disable requests
- Soft delete supported (hidden, not removed from DB)

**Ride Joining:**
- Request → Accept → Connect
- Confirmed passengers tracked

**Cost Handling:**
- Manual split by users
- No in-app payments (simulation only for demo)

---

## 4. CHAT & COMMUNICATION

**Chat System:**
- Enabled only after connection
- Text-only (Phase 1)

**AI Moderation System:**
- Fully automated (NO human reading chats)
- Encrypted chat storage
- Admin can only see **flagged messages**

**Content Filtering (Efficient Approach):**
- Regex detection:
  - Phone numbers
  - Emails
- Keyword filtering:
  - “call me”, “WhatsApp”, etc.
- Lightweight NLP filtering
- AI fallback (if needed, not primary)

**Privacy Enforcement:**
- Contact sharing restricted
- Contact unlock is a **premium feature**

---

## 5. LOCATION & MAPS

**Integration:**
- Use lightweight map APIs (e.g., Google Maps free tier)

**Features:**
- Location selection via map
- Route preview
- Text + map hybrid input

**Real-time tracking:**
- Not included (Phase 1)

---

## 6. AUTHENTICATION & SECURITY

**Authentication:**
- Email + OTP

**Security Measures:**
- Encrypted chat storage
- Audit logs for all actions
- Role-based access control

**Abuse Handling:**
- Reporting system
- Auto-flag after threshold (e.g., 10 reports)
- Admin review + action

---

## 7. BACKEND ARCHITECTURE (STRICT MICROSERVICES)

**Architecture Style:**
- Microservices (from Day 1 as per requirement)

**Core Services:**
- User Service
- Ride Service
- Chat Service
- Notification Service
- Admin/Analytics Service
- Moderation Service

**Tech Stack:**
- Backend: Spring Boot (Microservices)
- Communication: REST + WebSockets (chat)
- Database: PostgreSQL (per service or shared with schema isolation)

**Scalability:**
- Designed for horizontal scaling
- Service isolation for future growth

---

## 8. MOBILE APPLICATION

**Framework:**
- React Native

**Platforms:**
- Android + iOS (Day 1)

**UI Level:**
- Production-grade (ERP-level)
- Dark mode supported

---

## 9. DATA & STORAGE

**Stored Data:**
- Users
- Rides
- Connections
- Chats
- Reports
- Audit logs

**Database:**
- PostgreSQL

**File Storage:**
- Ride proof uploads (cloud storage)

**Retention Policy:**
- User-deleted data → hidden but retained in audit logs

---

## 10. MONETIZATION MODEL

**Revenue Streams:**
- Subscription model

**Premium Features:**
- Full profile access
- Contact details visibility

**Payments:**
- Outside app (UPI/Cash)
- In-app payment simulated only (demo purpose)

---

## 11. ADMIN PANEL (WEB-BASED ERP DASHBOARD)

**Platform:**
- Web dashboard (NOT mobile-only)

**Capabilities:**
- User management
- Ride monitoring
- Reports & analytics
- Flagged content review

**Analytics:**
- Real-time dashboards
- Export to PDF/Excel

---

## 12. NOTIFICATIONS

**Type:**
- Push notifications

**Triggers:**
- Connection requests
- Accept/reject
- Messages

---

## 13. SCALABILITY & FUTURE

- Multi-campus expansion
- City-level scaling
- Advanced recommendation system (route-based filtering)
- Multi-language support

---

## 14. TESTING STRATEGY

- Automated testing (device-based via USB)
- Integration + UI testing

---

## 15. LEGAL & COMPLIANCE

- Privacy policy
- Terms & conditions
- Data protection alignment

---

## 16. UX & EXPERIENCE

**Design Style:**
- Social media + ride sharing hybrid

**Core UX Features:**
- Feed-based ride posts
- Search & filters
- Structured journey flow

---

## 17. ANALYTICS

- Active users
- Ride success rate
- User behavior tracking

---

## 18. EDGE CASE HANDLING

**Cancellations / No-show / Disputes:**
- No real money penalties
- Use rating + reporting system
- Admin intervention via reports

**Chat Misuse:**
- Auto moderation + reporting
- Profile warnings / bans

---

## 19. FINAL POSITIONING

This system is a **production-grade, scalable ERP-level microservices architecture** with:
- Strong privacy controls
- AI-assisted moderation
- Real-time interaction
- Startup scalability

---

**Status:** Ready for System Design Phase (DB + APIs + Architecture Diagram)

