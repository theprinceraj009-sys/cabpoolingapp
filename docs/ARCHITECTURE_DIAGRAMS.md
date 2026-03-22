# CAMPUS TAXI POOLING — ARCHITECTURE DIAGRAMS

> **Document Type:** System Architecture Reference  
> **Scope:** High-Level Architecture + Low-Level Service Design  
> **Stack:** React Native · Spring Boot Microservices · PostgreSQL · WebSockets · Push Notifications  
> **Phase:** Pre-Development Analysis → System Design Phase

---

## DIAGRAM 1 — SYSTEM CONTEXT DIAGRAM (Bird's Eye View)

> Who are the actors and what external systems touch our platform?

```mermaid
graph TD
    subgraph External_Actors["👥 External Actors"]
        STU["🧑‍🎓 Student / Faculty\n(Mobile App User)"]
        ADM["🛡️ Admin\n(Web Dashboard)"]
    end

    subgraph External_Services["🌐 External Systems"]
        GMAPS["🗺️ Google Maps API\n(Route Preview / Location)"]
        FCM["🔔 Firebase FCM\n(Push Notifications)"]
        EMAIL["📧 SMTP / Email Service\n(OTP Verification)"]
        CLOUD["☁️ Cloud Storage\n(Ride Proof Uploads)"]
    end

    subgraph Platform["🏗️ CampusTaxiPooling Platform"]
        MOBILE["📱 React Native\nMobile App"]
        WEB["💻 Admin Web\nDashboard"]
        BACKEND["⚙️ Spring Boot\nMicroservices Backend"]
        DB[("🗃️ PostgreSQL\nDatabases")]
    end

    STU --> MOBILE
    ADM --> WEB
    MOBILE --> BACKEND
    WEB --> BACKEND
    BACKEND --> DB
    BACKEND <--> GMAPS
    BACKEND --> FCM
    BACKEND --> EMAIL
    BACKEND --> CLOUD
```

---

## DIAGRAM 2 — HIGH-LEVEL ARCHITECTURE DIAGRAM

> How are the major system layers connected?

```mermaid
graph TD
    subgraph CLIENT_LAYER["📱 Client Layer"]
        APP["React Native App\nAndroid + iOS"]
        ADMINWEB["Admin Web Dashboard\nBrowser"]
    end

    subgraph GATEWAY_LAYER["🔀 API Gateway Layer"]
        GW["API Gateway\nRate Limiting · Auth Filter · Routing · SSL Termination"]
    end

    subgraph SERVICE_LAYER["⚙️ Microservices Layer  — Spring Boot"]
        US["👤 User Service\nAuth · Profile · Roles"]
        RS["🚗 Ride Service\nPost · Join · Seat Mgmt"]
        CS["💬 Chat Service\nWebSocket · Encrypted Msg"]
        NS["🔔 Notification Service\nPush · In-App Alerts"]
        MS["🤖 Moderation Service\nRegex · NLP · Flagging"]
        AS["📊 Admin & Analytics\nDashboard API · Reports"]
    end

    subgraph DATA_LAYER["🗃️ Data Layer"]
        USERDB[("users_db\nPostgreSQL")]
        RIDEDB[("rides_db\nPostgreSQL")]
        CHATDB[("chats_db\nPostgreSQL")]
        ADMINDB[("admin_db\nPostgreSQL")]
        REDIS[("Redis Cache\nSessions · Feed Cache")]
    end

    subgraph EXTERNAL["🌐 External Services"]
        FCM2["Firebase FCM"]
        MAPS["Google Maps API"]
        SMTP["Email / SMTP"]
        S3["Cloud Storage\nRide Proof Files"]
    end

    APP <-->|"HTTPS REST"| GW
    ADMINWEB <-->|"HTTPS REST"| GW
    APP <-->|"WSS WebSocket"| CS

    GW --> US
    GW --> RS
    GW --> AS

    US --> USERDB
    US --> SMTP
    US --> REDIS

    RS --> RIDEDB
    RS --> MAPS
    RS --> S3
    RS -->|"Trigger Event"| NS
    RS -->|"Trigger Event"| MS

    CS --> CHATDB
    CS -->|"Flag Check"| MS
    CS -->|"Trigger Event"| NS

    MS --> ADMINDB
    NS --> FCM2

    AS --> ADMINDB
    AS --> USERDB
    AS --> RIDEDB
```

---

## DIAGRAM 3 — LOW-LEVEL ARCHITECTURE DIAGRAM

> Internal design of each microservice and the data they own

```mermaid
graph LR
    subgraph US_BOX["👤 USER SERVICE"]
        direction TB
        UA["AuthController\nPOST /auth/send-otp\nPOST /auth/verify-otp"]
        UB["ProfileController\nGET /users/me\nPUT /users/me\nGET /users/:id"]
        UC["UserRepository\n→ users table"]
        UD[("users_db\nusers\notp_tokens\nsubscriptions\naudit_log")]
        UA --> UC --> UD
        UB --> UC
    end

    subgraph RS_BOX["🚗 RIDE SERVICE"]
        direction TB
        RA["RideController\nPOST /rides\nGET /rides\nPATCH /rides/:id\nDELETE /rides/:id"]
        RB["ConnectionController\nPOST /rides/:id/request\nPATCH /rides/:id/request/:uid/accept\nGET /rides/:id/passengers"]
        RC["RideRepository\n→ rides table"]
        RD["ConnectionRepository\n→ connections table"]
        RE[("rides_db\nrides\nseat_requests\nconnections\nride_proof_files")]
        RA --> RC --> RE
        RB --> RD --> RE
    end

    subgraph CS_BOX["💬 CHAT SERVICE"]
        direction TB
        CA["WebSocket Handler\nws://connect\nws://send\nws://disconnect"]
        CB["MessageController\nGET /chats/:connectionId/history"]
        CC["MessageRepository\n→ messages table"]
        CD["EncryptionLayer\nAES-256 per connection"]
        CE[("chats_db\nmessages\nchat_sessions")]
        CA --> CD --> CC --> CE
        CB --> CC
    end

    subgraph MS_BOX["🤖 MODERATION SERVICE"]
        direction TB
        MA["ModerationController\nPOST /moderate/message\nGET /moderate/flagged"]
        MB["RegexEngine\nPhone · Email detection"]
        MC["KeywordFilter\n'call me' · 'WhatsApp' etc"]
        MD["NLP Engine\nLightweight classification"]
        ME[("admin_db\nflagged_messages\nmoderation_log")]
        MA --> MB --> ME
        MA --> MC --> ME
        MA --> MD --> ME
    end

    subgraph NS_BOX["🔔 NOTIFICATION SERVICE"]
        direction TB
        NA["NotificationController\nPOST /notify/push\nGET /notify/history/:uid"]
        NB["FCMAdapter\nFirebase Push"]
        NC["InAppAdapter\nIn-app alert storage"]
        ND[("admin_db\nnotifications")]
        NA --> NB
        NA --> NC --> ND
    end

    subgraph AS_BOX["📊 ADMIN & ANALYTICS SERVICE"]
        direction TB
        AA["AdminController\nGET /admin/users\nPATCH /admin/users/:id/ban\nGET /admin/rides\nGET /admin/reports\nGET /admin/flagged"]
        AB["AnalyticsController\nGET /analytics/dashboard\nGET /analytics/export"]
        AC[("Read access:\nusers_db\nrides_db\nadmin_db")]
        AA --> AC
        AB --> AC
    end
```

---

## DIAGRAM 4 — RIDE WORKFLOW (Sequence Diagram)

> Step-by-step flow from ride post to confirmed connection

```mermaid
sequenceDiagram
    actor UserA as 🧑 User A (Poster)
    actor UserB as 👤 User B (Joiner)
    participant GW as API Gateway
    participant RS as Ride Service
    participant NS as Notification Service
    participant CS as Chat Service

    UserA->>GW: POST /rides (source, dest, time, fare, seats)
    GW->>RS: Forward request + JWT
    RS->>RS: Validate + persist ride
    RS-->>UserA: 201 Created { rideId }

    UserB->>GW: GET /rides (browse feed)
    GW->>RS: Forward
    RS-->>UserB: 200 [ ride list ]

    UserB->>GW: POST /rides/{rideId}/request
    GW->>RS: Forward request + JWT
    RS->>RS: Create PENDING seat request
    RS->>NS: Emit event: "NEW_JOIN_REQUEST"
    NS-->>UserA: 🔔 Push Notification

    UserA->>GW: PATCH /rides/{rideId}/request/{userId}/accept
    GW->>RS: Forward
    RS->>RS: Status → ACCEPTED, decrement seats
    RS->>NS: Emit event: "REQUEST_ACCEPTED"
    NS-->>UserB: 🔔 Push Notification

    RS->>CS: Emit event: "UNLOCK_CHAT" { connectionId }
    CS->>CS: Create encrypted chat session

    UserB-->>CS: 💬 Chat now available
    UserA-->>CS: 💬 Chat now available
```

---

## DIAGRAM 5 — CHAT + MODERATION WORKFLOW (Sequence Diagram)

> How a message travels through the safety layer before delivery

```mermaid
sequenceDiagram
    actor Sender as 🧑 Sender
    actor Receiver as 👤 Receiver
    participant CS as Chat Service
    participant MS as Moderation Service
    participant DB as chats_db
    participant NS as Notification Service
    participant AdminDB as admin_db (Flagged)

    Sender->>CS: ws://send { connectionId, message }
    CS->>MS: POST /moderate/message { payload }

    alt Message is CLEAN
        MS-->>CS: { status: CLEAN }
        CS->>CS: Encrypt message (AES-256)
        CS->>DB: Store encrypted message
        CS->>NS: Emit: "NEW_MESSAGE"
        NS-->>Receiver: 🔔 Push Notification
        CS-->>Receiver: ws delivery (if online)
    else Message is FLAGGED
        MS-->>CS: { status: FLAGGED, reason: "phone_detected" }
        CS-->>Sender: ⚠️ Message blocked (warn)
        MS->>AdminDB: Store flagged message + context
        MS->>NS: Emit: "FLAG_COUNT_THRESHOLD_CHECK"
    end
```

---

## DIAGRAM 6 — AUTHENTICATION FLOW (Sequence Diagram)

> OTP-based email verification and JWT issuance

```mermaid
sequenceDiagram
    actor User as 🧑 User
    participant APP as Mobile App
    participant GW as API Gateway
    participant US as User Service
    participant SMTP as Email / SMTP
    participant Cache as Redis / OTP Store

    User->>APP: Enter college email address
    APP->>GW: POST /auth/send-otp { email }
    GW->>US: Forward
    US->>US: Validate domain (e.g. @cuchd.in)
    US->>Cache: Store OTP (TTL: 5 min)
    US->>SMTP: Send OTP email
    SMTP-->>User: 📧 OTP received

    User->>APP: Enter OTP
    APP->>GW: POST /auth/verify-otp { email, otp }
    GW->>US: Forward
    US->>Cache: Validate OTP + expiry

    alt OTP Valid
        US->>US: Issue JWT (access + refresh token)
        US-->>APP: 200 { accessToken, refreshToken, userProfile }
    else OTP Invalid / Expired
        US-->>APP: 401 { error: "Invalid or expired OTP" }
    end
```

---

## DIAGRAM 7 — DATA MODEL OVERVIEW (Entity Relationship)

> Core entities and their relationships across services

```mermaid
erDiagram
    USERS {
        uuid id PK
        string name
        string email
        string uid
        string department
        enum role
        enum subscription_tier
        boolean is_banned
        timestamp created_at
    }

    RIDES {
        uuid id PK
        uuid posted_by_user_id FK
        string source
        string destination
        string route_description
        timestamp journey_time
        int total_fare
        int total_seats
        int seats_remaining
        enum status
        boolean is_deleted
        timestamp created_at
    }

    SEAT_REQUESTS {
        uuid id PK
        uuid ride_id FK
        uuid requester_user_id FK
        enum status
        timestamp requested_at
    }

    CONNECTIONS {
        uuid id PK
        uuid ride_id FK
        uuid user_a_id FK
        uuid user_b_id FK
        timestamp connected_at
    }

    MESSAGES {
        uuid id PK
        uuid connection_id FK
        uuid sender_id FK
        text encrypted_body
        boolean is_flagged
        timestamp sent_at
    }

    FLAGGED_MESSAGES {
        uuid id PK
        uuid message_id FK
        string flag_reason
        enum admin_action
        timestamp flagged_at
    }

    AUDIT_LOG {
        uuid id PK
        uuid user_id FK
        string action
        jsonb metadata
        timestamp performed_at
    }

    USERS ||--o{ RIDES : "posts"
    USERS ||--o{ SEAT_REQUESTS : "makes"
    RIDES ||--o{ SEAT_REQUESTS : "receives"
    RIDES ||--o{ CONNECTIONS : "creates"
    CONNECTIONS ||--o{ MESSAGES : "contains"
    MESSAGES ||--o| FLAGGED_MESSAGES : "may produce"
    USERS ||--o{ AUDIT_LOG : "generates"
```

---

## DIAGRAM 8 — DEPLOYMENT TOPOLOGY

> How services map to infrastructure in Phase 1

```mermaid
graph TB
    subgraph INTERNET["🌐 Internet"]
        MOBILE2["📱 React Native App"]
        BROWSER["💻 Admin Browser"]
    end

    subgraph CLOUD_INFRA["☁️ Cloud Infrastructure (Phase 1 — Single Region)"]
        subgraph EDGE["Edge Layer"]
            CDN["CDN / TLS Termination"]
            LB["Load Balancer"]
        end

        subgraph COMPUTE["Compute — Docker Containers"]
            GW2["API Gateway Container"]
            SVC1["User Service Container"]
            SVC2["Ride Service Container"]
            SVC3["Chat Service Container (WebSocket)"]
            SVC4["Notification Service Container"]
            SVC5["Moderation Service Container"]
            SVC6["Admin Service Container"]
        end

        subgraph STORAGE["Storage Layer"]
            PG1[("PostgreSQL\nusers_db")]
            PG2[("PostgreSQL\nrides_db")]
            PG3[("PostgreSQL\nchats_db")]
            PG4[("PostgreSQL\nadmin_db")]
            RDS["Redis\nSession Cache / Feed Cache"]
            BLOB["Cloud Blob Storage\nRide Proofs"]
        end

        subgraph EXT_SVC["Managed External Services"]
            FCM3["Firebase FCM"]
            MAPS2["Google Maps API"]
            SMTP2["SMTP Provider"]
        end
    end

    MOBILE2 --> CDN --> LB --> GW2
    BROWSER --> CDN

    GW2 --> SVC1 & SVC2 & SVC3 & SVC4 & SVC5 & SVC6

    SVC1 --> PG1 & RDS & SMTP2
    SVC2 --> PG2 & MAPS2 & BLOB
    SVC3 --> PG3 & RDS
    SVC4 --> FCM3 & PG4
    SVC5 --> PG4
    SVC6 --> PG1 & PG2 & PG4
```

---

## SUMMARY TABLE

| Service | Owns DB | Protocol | External Dep | Triggers |
|---|---|---|---|---|
| User Service | `users_db` | REST | SMTP | — |
| Ride Service | `rides_db` | REST | Google Maps, Cloud Storage | Notification, Chat |
| Chat Service | `chats_db` | WebSocket + REST | — | Moderation, Notification |
| Moderation Service | `admin_db` | REST (internal) | — | Admin review, NS threshold |
| Notification Service | `admin_db` | REST (internal) | Firebase FCM | — |
| Admin & Analytics | Read-all DBs | REST | — | — |

---

> **Author:** Architecture Analysis Phase  
> **Date:** 2026-03-22  
> **Status:** Ready for DB Schema + API Contract population
