# Part 6: Securing Microservices with Keycloak

![1766248556386](image/part6_Security_using_Keyloak/1766248556386.png)

Article : [link](https://www.altkomsoftware.com/blog/keycloak-security-in-microservices/#:~:text=Keycloak%20is%20an%20OAuth2%20and,each%20microservice%20and%20frontend%20application.)
---

## 📋 Table of Contents

1. [Introduction to Keycloak](#1-introduction-to-keycloak)
2. [Architecture Overview](#2-architecture-overview)
3. [Setting Up Keycloak with Docker](#3-setting-up-keycloak-with-docker)
4. [Core Concepts](#4-core-concepts)
5. [Keycloak Configuration](#5-keycloak-configuration)
6. [Spring Boot Gateway Configuration](#6-spring-boot-gateway-configuration)
7. [Access Token vs Refresh Token](#7-access-token-vs-refresh-token)
8. [Testing with Postman](#8-testing-with-postman)
9. [Frontend Integration (Custom Login Page)](#9-frontend-integration-custom-login-page)
10. [Token Refresh Strategies](#10-token-refresh-strategies)
11. [Role-Based Access Control](#11-role-based-access-control)
12. [Getting Current User in Backend](#12-getting-current-user-in-backend)
13. [Complete Flow Summary](#13-complete-flow-summary)
14. [Why Keycloak for Microservices?](#14-why-keycloak-for-microservices)
15. [Authentication vs Authorization](#15-authentication-vs-authorization)
16. [JWT Token Deep Dive](#16-jwt-token-deep-dive)
17. [Client Types in Keycloak](#17-client-types-in-keycloak)
18. [Advanced Authorization (Policies & Permissions)](#18-advanced-authorization-with-policies--permissions)
19. [Production Best Practices](#19-production-best-practices)
20. [HTTP Status Codes Reference](#20-http-status-codes-reference)

---

## 1. Introduction to Keycloak

**Keycloak** is an open-source Identity and Access Management (IAM) solution developed by Red Hat.

### Key Features

| Feature | Description |
|---------|-------------|
| 🔐 **Single Sign-On (SSO)** | Users authenticate once and access multiple applications |
| 🌐 **Identity Brokering** | Connect with Google, Facebook, GitHub, LDAP |
| 👥 **User Federation** | Sync users from external directories |
| 📜 **OAuth 2.0 & OIDC** | Industry-standard protocols |
| 🎭 **Role-Based Access Control** | Fine-grained permission management |

> 📖 **Reference**: [Spring Boot Microservices Tutorial - Security](https://programmingtechie.com/articles/spring-boot-microservices-tutorial-part-)

---

## 2. Architecture Overview

We use the **Direct Authentication Approach** - the most common and recommended pattern:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         DIRECT AUTHENTICATION ARCHITECTURE                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌──────────────┐                              ┌────────────────────┐          │
│   │   FRONTEND   │                              │      KEYCLOAK      │          │
│   │  (React/JS)  │                              │   localhost:8181   │          │
│   └──────┬───────┘                              └─────────┬──────────┘          │
│          │                                                │                      │
│          │  1. POST /token (username, password)           │                      │
│          │───────────────────────────────────────────────►│                      │
│          │                                                │                      │
│          │  2. { access_token, refresh_token }            │                      │
│          │◄───────────────────────────────────────────────│                      │
│          │                                                │                      │
│          │  3. Store in localStorage                      │                      │
│          │                                                │                      │
│          │  4. GET /api/products                          │                      │
│          │     Authorization: Bearer <token>              │                      │
│          │───────────────────────────┐                    │                      │
│          │                           │                    │                      │
│          │                    ┌──────▼───────┐            │                      │
│          │                    │   GATEWAY    │  5. Validate JWT                  │
│          │                    │ localhost:9000│───────────►│                      │
│          │                    └──────┬───────┘◄───────────│                      │
│          │                           │        (public keys)                      │
│          │                    ┌──────▼───────┐                                   │
│          │  6. Response       │ MICROSERVICE │                                   │
│          │◄───────────────────┤  (products)  │                                   │
│          │                    └──────────────┘                                   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Why This Approach?

| Benefit | Description |
|---------|-------------|
| ✅ **Simple** | Less code in your Gateway |
| ✅ **Standard OAuth2** | Industry best practice |
| ✅ **Scalable** | Keycloak handles auth load |
| ✅ **Stateless** | No session management needed |

---

## 3. Setting Up Keycloak with Docker

### Docker Compose Configuration

```yaml
name: keycloak-auth-service
version: '3.8'
services:
  keycloak-mysql:
    container_name: keycloak-mysql
    image: mysql:8
    volumes:
      - ./volume-data/mysql_keycloak_data:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: keycloak
      MYSQL_USER: keycloak
      MYSQL_PASSWORD: password

  keycloak:
    container_name: keycloak
    image: quay.io/keycloak/keycloak:24.0.1
    command: [ "start-dev", "--import-realm" ]
    environment:
      DB_VENDOR: MYSQL
      DB_ADDR: mysql
      DB_DATABASE: keycloak
      DB_USER: keycloak
      DB_PASSWORD: password
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8181:8080"
    volumes:
      - ./docker/keycloak/realms/:/opt/keycloak/data/import/
    depends_on:
      - keycloak-mysql
```

### Quick Reference

| Service | Port | Credentials |
|---------|------|-------------|
| Keycloak Admin Console | `http://localhost:8181` | admin / admin |
| MySQL Database | 3306 | keycloak / password |

![1766601245466](image/part6_Security_using_Keyloak/1766601245466.png)

---

## 4. Core Concepts

### 🔑 Realm
A **tenant** in Keycloak. Isolates users, roles, and clients.

| Realm | Purpose |
|-------|---------|
| `master` | Admin realm (don't use for apps) |
| `spring-microservices-security-realm` | Your application realm |

### 👤 Users
End-users who authenticate. Stored with credentials and profile info.

### 🎭 Roles
Define permissions:
- **Realm Roles**: Global across all clients (e.g., `ADMIN`, `USER`)
- **Client Roles**: Specific to one application

### 📱 Clients
Applications that request authentication:
- **Public Client**: Browser apps (no secret) - ✅ For frontend
- **Confidential Client**: Server apps (with secret) - For backend-to-backend

---

## 5. Keycloak Configuration

### Step 1: Create a Realm

1. Open `http://localhost:8181`
2. Login: `admin` / `admin`
3. Click realm dropdown → **Create Realm**
4. Name: `spring-microservices-security-realm`
5. Click **Create**

![1766602033530](image/part6_Security_using_Keyloak/1766602033530.png)

### Step 2: Create Roles

Go to **Realm roles** → **Create role**

| Role Name | Description |
|-----------|-------------|
| `ADMIN` | Full access to all resources |
| `USER` | Standard user access |

### Step 3: Create a Public Client (for Frontend)

Go to **Clients** → **Create client**

| Setting | Value | Why |
|---------|-------|-----|
| Client ID | `frontend-app` | Identifier for your frontend |
| Client authentication | ⭕ **OFF** | Public client (no secret needed) |
| Standard flow | ☐ OFF | Not using redirection |
| **Direct access grants** | ☑ **ON** | ✅ Enables Password Grant (custom login) |

Click **Save**

> 💡 **Public Client**: Since JavaScript code is visible in browsers, we can't hide a secret. Public clients authenticate using only the `client_id`.

### Step 4: Create Test Users

Go to **Users** → **Add user**

#### User 1: Amine (Admin)
| Field | Value |
|-------|-------|
| Username | `amine` |
| Email | `amine@example.com` |
| Email verified | ✅ ON |

Click **Create** → **Credentials** tab:
- Set Password: `amine123`
- Temporary: **OFF**

Go to **Role mapping** → **Assign role**:
- Select `ADMIN` and `USER`

#### User 2: Sara (Regular User)
| Field | Value |
|-------|-------|
| Username | `sara` |
| Password | `sara123` |
| Roles | `USER` only |

### Step 5: Get OpenID Configuration

Go to **Realm Settings** → **General** → Click **OpenID Endpoint Configuration**

Important endpoints:
```
issuer:          http://localhost:8181/realms/spring-microservices-security-realm
token_endpoint:  .../protocol/openid-connect/token
jwks_uri:        .../protocol/openid-connect/certs
```

---

## 6. Spring Boot Gateway Configuration

### Step 1: Add Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### Step 2: Configure Application Properties

```properties
spring.application.name=gateway-service
server.port=9000
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8181/realms/spring-microservices-security-realm
```

> 💡 Using `issuer-uri` lets Spring auto-discover the JWK Set URI for token validation.

### Step 3: Security Configuration

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()))
                .build();
    }
}
```

| Configuration | Purpose |
|--------------|---------|
| `.anyRequest().authenticated()` | All endpoints require valid JWT |
| `.oauth2ResourceServer()` | This service validates tokens (Resource Server) |
| `.jwt()` | Expect JWT tokens |

---

## 7. Access Token vs Refresh Token

### The Difference

| Feature | Access Token | Refresh Token |
|---------|--------------|---------------|
| **Purpose** | Authorize API requests | Get new access tokens |
| **Lifetime** | Short (5 min) | Long (30 min - 24h) |
| **Sent to** | Gateway / APIs | Only to Keycloak |
| **Contains** | User info, roles | Just a reference ID |
| **If stolen** | Limited damage (expires soon) | Can get new tokens |

### Visual Explanation

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         TOKEN LIFECYCLE                                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Timeline:                                                                       │
│  ════════════════════════════════════════════════════════════════════════════   │
│                                                                                  │
│  0:00 ──► Login                                                                  │
│           └─► Get access_token (expires in 5 min)                               │
│               Get refresh_token (expires in 30 min)                             │
│                                                                                  │
│  0:30 ──► User clicks "Load Products"                                           │
│           └─► Token valid ✅ → Call API                                         │
│                                                                                  │
│  5:30 ──► User clicks "Add to Cart"                                             │
│           └─► Token expired ❌ → Use refresh_token → Get new access_token       │
│               └─► Call API with new token ✅                                    │
│                                                                                  │
│  35:00 ─► User returns after break                                              │
│           └─► Token expired ❌ → Refresh also expired ❌                        │
│               └─► Redirect to login                                             │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Testing with Postman

### 8.1 Get Access Token (Login)

**Request:**
```http
POST http://localhost:8181/realms/spring-microservices-security-realm/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded
```

**Body (x-www-form-urlencoded):**

| Key | Value |
|-----|-------|
| grant_type | `password` |
| client_id | `frontend-app` |
| username | `amine` |
| password | `amine123` |
| scope | `openid profile email` |

> ⚠️ No `client_secret` needed for public client!

**Response:**
```json
{
    "access_token": "eyJhbGciOiJSUzI1NiIs...",
    "expires_in": 300,
    "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
    "refresh_expires_in": 1800,
    "token_type": "Bearer"
}
```

### 8.2 Call API with Token

```http
GET http://localhost:9000/api/products
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

### 8.3 Refresh Token

```http
POST http://localhost:8181/realms/spring-microservices-security-realm/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded
```

| Key | Value |
|-----|-------|
| grant_type | `refresh_token` |
| client_id | `frontend-app` |
| refresh_token | `eyJhbGciOiJIUzI1NiIs...` |

### 8.4 Postman Environment Variables (Pro Tip)

Add this to **Tests** tab of login request:

```javascript
if (pm.response.code === 200) {
    var data = pm.response.json();
    pm.environment.set("access_token", data.access_token);
    pm.environment.set("refresh_token", data.refresh_token);
}
```

Then use in requests:
```
Authorization: Bearer {{access_token}}
```

![1766605635005](image/part6_Security_using_Keyloak/1766605635005.png)
![1766607036752](image/part6_Security_using_Keyloak/1766607036752.png)

---

## 9. Frontend Integration (Custom Login Page)

### 9.1 Project Structure

```
frontend/
├── index.html
├── login.html
├── dashboard.html
└── js/
    ├── config.js      ← Keycloak settings
    ├── auth.js        ← Login/logout/refresh
    └── api.js         ← API calls with token
```

### 9.2 Configuration

**js/config.js**
```javascript
const CONFIG = {
    KEYCLOAK_URL: 'http://localhost:8181',
    REALM: 'spring-microservices-security-realm',
    CLIENT_ID: 'frontend-app',
    API_BASE_URL: 'http://localhost:9000',
    
    get TOKEN_ENDPOINT() {
        return `${this.KEYCLOAK_URL}/realms/${this.REALM}/protocol/openid-connect/token`;
    }
};
```

### 9.3 Authentication Module

**js/auth.js**
```javascript
const Auth = {
    
    // ═══════════════════════════════════════════════
    // LOGIN - Get tokens from Keycloak
    // ═══════════════════════════════════════════════
    async login(username, password) {
        const response = await fetch(CONFIG.TOKEN_ENDPOINT, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                grant_type: 'password',
                client_id: CONFIG.CLIENT_ID,
                username: username,
                password: password,
                scope: 'openid profile email'
            })
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error_description || 'Login failed');
        }
        
        const tokens = await response.json();
        this.saveTokens(tokens);
        return this.getUser();
    },
    
    // ═══════════════════════════════════════════════
    // LOGOUT - Clear tokens
    // ═══════════════════════════════════════════════
    logout() {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('token_expiry');
        window.location.href = '/login.html';
    },
    
    // ═══════════════════════════════════════════════
    // REFRESH TOKEN - Get new access token
    // ═══════════════════════════════════════════════
    async refreshToken() {
        const refreshToken = localStorage.getItem('refresh_token');
        if (!refreshToken) return false;
        
        try {
            const response = await fetch(CONFIG.TOKEN_ENDPOINT, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({
                    grant_type: 'refresh_token',
                    client_id: CONFIG.CLIENT_ID,
                    refresh_token: refreshToken
                })
            });
            
            if (response.ok) {
                const tokens = await response.json();
                this.saveTokens(tokens);
                return true;
            }
        } catch (e) {
            console.error('Refresh failed:', e);
        }
        
        this.logout();
        return false;
    },
    
    // ═══════════════════════════════════════════════
    // TOKEN MANAGEMENT
    // ═══════════════════════════════════════════════
    saveTokens(tokens) {
        localStorage.setItem('access_token', tokens.access_token);
        localStorage.setItem('refresh_token', tokens.refresh_token);
        localStorage.setItem('token_expiry', Date.now() + (tokens.expires_in * 1000));
    },
    
    getAccessToken() {
        return localStorage.getItem('access_token');
    },
    
    isTokenExpired() {
        const expiry = localStorage.getItem('token_expiry');
        if (!expiry) return true;
        // Add 30-second buffer
        return Date.now() > (parseInt(expiry) - 30000);
    },
    
    isAuthenticated() {
        return this.getAccessToken() && !this.isTokenExpired();
    },
    
    // ═══════════════════════════════════════════════
    // GET USER FROM TOKEN
    // ═══════════════════════════════════════════════
    getUser() {
        const token = this.getAccessToken();
        if (!token) return null;
        
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return {
                id: payload.sub,
                username: payload.preferred_username,
                email: payload.email,
                name: payload.name || payload.preferred_username,
                roles: payload.realm_access?.roles || []
            };
        } catch (e) {
            return null;
        }
    },
    
    hasRole(role) {
        return this.getUser()?.roles?.includes(role) || false;
    },
    
    isAdmin() {
        return this.hasRole('ADMIN');
    }
};
```

### 9.4 API Module with Auto-Refresh

**js/api.js**
```javascript
const API = {
    
    // ═══════════════════════════════════════════════
    // FETCH WITH AUTH - Auto-refreshes token if needed
    // ═══════════════════════════════════════════════
    async request(endpoint, options = {}) {
        // Check if token needs refresh
        if (Auth.isTokenExpired()) {
            const refreshed = await Auth.refreshToken();
            if (!refreshed) return;
        }
        
        const token = Auth.getAccessToken();
        
        const response = await fetch(`${CONFIG.API_BASE_URL}${endpoint}`, {
            ...options,
            headers: {
                ...options.headers,
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        
        // Handle 401 - Token rejected
        if (response.status === 401) {
            const refreshed = await Auth.refreshToken();
            if (refreshed) return this.request(endpoint, options);
        }
        
        return response;
    },
    
    // Convenience methods
    async get(endpoint) {
        const res = await this.request(endpoint);
        return res?.json();
    },
    
    async post(endpoint, data) {
        const res = await this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(data)
        });
        return res?.json();
    }
};
```

### 9.5 Login Page

**login.html**
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Segoe UI', sans-serif;
            background: linear-gradient(135deg, #1a1a2e, #16213e);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .login-box {
            background: rgba(255,255,255,0.05);
            backdrop-filter: blur(10px);
            padding: 40px;
            border-radius: 16px;
            width: 100%;
            max-width: 400px;
            border: 1px solid rgba(255,255,255,0.1);
        }
        h1 { color: #fff; text-align: center; margin-bottom: 30px; }
        input {
            width: 100%;
            padding: 14px;
            margin-bottom: 16px;
            background: rgba(255,255,255,0.1);
            border: 1px solid rgba(255,255,255,0.2);
            border-radius: 8px;
            color: #fff;
            font-size: 16px;
        }
        input:focus { outline: none; border-color: #4f46e5; }
        button {
            width: 100%;
            padding: 14px;
            background: linear-gradient(135deg, #4f46e5, #7c3aed);
            border: none;
            border-radius: 8px;
            color: #fff;
            font-size: 16px;
            cursor: pointer;
        }
        button:hover { transform: translateY(-2px); }
        .error {
            background: rgba(239,68,68,0.2);
            color: #fca5a5;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 16px;
            display: none;
        }
    </style>
</head>
<body>
    <div class="login-box">
        <h1>🔐 Login</h1>
        <div id="error" class="error"></div>
        <form id="loginForm">
            <input type="text" id="username" placeholder="Username" required>
            <input type="password" id="password" placeholder="Password" required>
            <button type="submit">Sign In</button>
        </form>
    </div>
    
    <script src="js/config.js"></script>
    <script src="js/auth.js"></script>
    <script>
        // Redirect if already logged in
        if (Auth.isAuthenticated()) {
            window.location.href = '/dashboard.html';
        }
        
        document.getElementById('loginForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const errorDiv = document.getElementById('error');
            
            try {
                const user = await Auth.login(
                    document.getElementById('username').value,
                    document.getElementById('password').value
                );
                
                // Redirect based on role
                window.location.href = Auth.isAdmin() ? '/admin.html' : '/dashboard.html';
                
            } catch (err) {
                errorDiv.textContent = err.message;
                errorDiv.style.display = 'block';
            }
        });
    </script>
</body>
</html>
```

---

## 10. Token Refresh Strategies

### ❌ Bad: Scheduled Refresh

```javascript
// Don't do this - wastes resources
setInterval(() => refreshToken(), 4 * 60 * 1000);
```

### ✅ Good: Lazy Refresh (Recommended)

Refresh **only when making an API call** and token is expired:

```
┌───────────────────────────────────────────────────────────────┐
│                    LAZY REFRESH FLOW                           │
├───────────────────────────────────────────────────────────────┤
│                                                                │
│  User clicks button → API.get('/products')                    │
│           │                                                    │
│           ▼                                                    │
│  ┌─────────────────────────────────┐                          │
│  │   Is access_token valid?        │                          │
│  └─────────────────────────────────┘                          │
│           │                                                    │
│     ┌─────┴─────┐                                             │
│    YES          NO                                             │
│     │            │                                             │
│     │     ┌──────▼──────────────────┐                         │
│     │     │ Call refresh endpoint   │                         │
│     │     │ with refresh_token      │                         │
│     │     └──────┬──────────────────┘                         │
│     │            │                                             │
│     │      ┌─────┴─────┐                                      │
│     │    SUCCESS      FAIL                                     │
│     │      │            │                                      │
│     │      │     ┌──────▼──────┐                              │
│     │      │     │ Redirect to │                              │
│     │      │     │   Login     │                              │
│     │      │     └─────────────┘                              │
│     ▼      ▼                                                   │
│  ┌────────────────────┐                                       │
│  │ Call API with      │                                       │
│  │ valid token        │                                       │
│  └────────────────────┘                                       │
│                                                                │
└───────────────────────────────────────────────────────────────┘
```

This is already implemented in our `api.js` above!

---

## 11. Role-Based Access Control

### Backend: Secure Endpoints by Role

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/public/**").permitAll()
                        // Admin only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Authenticated users
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                        // Everything else
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
                .build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = 
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }
}
```

### Frontend: Protect Routes

```javascript
// Check before loading protected pages
if (!Auth.isAuthenticated()) {
    window.location.href = '/login.html';
}

// Check admin access
if (!Auth.isAdmin()) {
    window.location.href = '/unauthorized.html';
}
```

---

## 12. Getting Current User in Backend

### Option 1: @AuthenticationPrincipal

```java
@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "userId", jwt.getSubject(),
            "username", jwt.getClaimAsString("preferred_username"),
            "email", jwt.getClaimAsString("email"),
            "roles", jwt.getClaimAsMap("realm_access").get("roles")
        );
    }
}
```

### Option 2: SecurityContextHolder

```java
@Service
public class UserService {
    
    public String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        return jwt.getClaimAsString("preferred_username");
    }
}
```

### JWT Token Structure

```json
{
    "sub": "user-uuid-12345",
    "preferred_username": "amine",
    "email": "amine@example.com",
    "name": "Amine",
    "realm_access": {
        "roles": ["ADMIN", "USER"]
    },
    "exp": 1703457600,
    "iat": 1703454000
}
```

---

## 13. Complete Flow Summary

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         COMPLETE AUTHENTICATION FLOW                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌────────────────────┐                                                         │
│  │  1. USER LOGIN     │                                                         │
│  │  ─────────────────  │                                                         │
│  │  Username: amine   │                                                         │
│  │  Password: ****    │                                                         │
│  └─────────┬──────────┘                                                         │
│            │                                                                     │
│            │ POST /token (grant_type=password)                                  │
│            ▼                                                                     │
│  ┌────────────────────┐                                                         │
│  │  2. KEYCLOAK       │                                                         │
│  │  ─────────────────  │                                                         │
│  │  Validates user    │                                                         │
│  │  Returns tokens    │                                                         │
│  └─────────┬──────────┘                                                         │
│            │ { access_token, refresh_token }                                    │
│            ▼                                                                     │
│  ┌────────────────────┐                                                         │
│  │  3. FRONTEND       │                                                         │
│  │  ─────────────────  │                                                         │
│  │  Stores tokens     │                                                         │
│  │  Decodes JWT       │                                                         │
│  │  Redirects user    │                                                         │
│  └─────────┬──────────┘                                                         │
│            │ GET /api/products (Authorization: Bearer token)                    │
│            ▼                                                                     │
│  ┌────────────────────┐                                                         │
│  │  4. GATEWAY        │─────────────┐                                           │
│  │  ─────────────────  │             │                                           │
│  │  Validates JWT     │             │ Fetch public keys                         │
│  │  Checks roles      │◄────────────┘                                           │
│  └─────────┬──────────┘                                                         │
│            │ Forward request                                                    │
│            ▼                                                                     │
│  ┌────────────────────┐                                                         │
│  │  5. MICROSERVICE   │                                                         │
│  │  ─────────────────  │                                                         │
│  │  @AuthPrincipal    │                                                         │
│  │  → "amine"         │                                                         │
│  │  Process request   │                                                         │
│  └────────────────────┘                                                         │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Quick Reference

| Action | How To |
|--------|--------|
| Login | POST `/token` with `grant_type=password` |
| Refresh | POST `/token` with `grant_type=refresh_token` |
| API Call | Header: `Authorization: Bearer <token>` |
| Get user (frontend) | Decode JWT: `atob(token.split('.')[1])` |
| Get user (backend) | `@AuthenticationPrincipal Jwt jwt` |
| Check role (frontend) | `user.realm_access.roles.includes('ADMIN')` |
| Check role (backend) | `.hasRole("ADMIN")` |

---

## 14. Why Keycloak for Microservices?

### The Security Challenge

When a system is divided into multiple microservices, **each component needs to be properly secured**. This is more difficult than securing a single monolithic application.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    MONOLITH vs MICROSERVICES SECURITY                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   MONOLITH                           MICROSERVICES                              │
│   ─────────                          ─────────────                              │
│   ┌─────────────────┐               ┌──────┐ ┌──────┐ ┌──────┐                 │
│   │                 │               │  🔐  │ │  🔐  │ │  🔐  │                 │
│   │   🔐 Single     │               │ User │ │Order │ │ Inv. │                 │
│   │   Security      │               │ Svc  │ │ Svc  │ │ Svc  │                 │
│   │   Point         │               └──────┘ └──────┘ └──────┘                 │
│   │                 │                   │        │        │                     │
│   └─────────────────┘                   └────────┼────────┘                     │
│                                                  │                              │
│   ✅ Easy to secure                    ┌─────────▼─────────┐                   │
│                                        │     KEYCLOAK       │                   │
│                                        │  Central Security  │                   │
│                                        └────────────────────┘                   │
│                                                                                  │
│                                        ✅ Centralized auth                      │
│                                        ✅ Consistent security                   │
│                                        ✅ Single point of config                │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Keycloak Benefits in Microservices

| Benefit | Description |
|---------|-------------|
| 🎯 **Centralized Security** | One place to manage all authentication |
| 🔄 **Consistent Across Services** | All services use the same JWT validation |
| 📝 **Declarative Configuration** | Define once, apply everywhere |
| 🔌 **Easy Integration** | Works over HTTP with any language |
| 📊 **Audit Trail** | Track all authentications in one place |

---

## 15. Authentication vs Authorization

### The Two Security Questions

| Question | Process | Example |
|----------|---------|---------|
| **Is the user who they say they are?** | Authentication | Login with username/password |
| **Does the user have permission?** | Authorization | Can user access `/admin` endpoint? |

### Visual Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     AUTHENTICATION → AUTHORIZATION                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   User: "I am Amine"                                                            │
│           │                                                                      │
│           ▼                                                                      │
│   ┌───────────────────────────┐                                                 │
│   │     AUTHENTICATION        │                                                 │
│   │  ─────────────────────    │                                                 │
│   │  ✓ Verify username        │                                                 │
│   │  ✓ Verify password        │                                                 │
│   │  ✓ Issue JWT token        │                                                 │
│   └─────────────┬─────────────┘                                                 │
│                 │                                                                │
│                 │  Token: { sub: "amine", roles: ["ADMIN"] }                    │
│                 ▼                                                                │
│   ┌───────────────────────────┐                                                 │
│   │     AUTHORIZATION         │                                                 │
│   │  ─────────────────────    │                                                 │
│   │  Request: GET /api/admin  │                                                 │
│   │  Required: ADMIN role     │                                                 │
│   │  User has: ADMIN role ✅  │                                                 │
│   │  → ACCESS GRANTED         │                                                 │
│   └───────────────────────────┘                                                 │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 16. JWT Token Deep Dive

### Token Structure

A JWT token consists of **3 parts** separated by dots:

```
eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhbWluZSJ9.signature
        │                    │                 │
     HEADER              PAYLOAD          SIGNATURE
```

### Each Part Explained

| Part | Contains | Purpose |
|------|----------|---------|
| **Header** | Algorithm, Token type | Metadata about the token |
| **Payload** | User info, Roles, Expiry | Claims about the user |
| **Signature** | Encrypted hash | Verifies token wasn't modified |

### Decoded Example

```json
// HEADER
{
    "alg": "RS256",      // Algorithm: RSA SHA-256
    "typ": "JWT"         // Token type
}

// PAYLOAD
{
    "iss": "http://localhost:8181/realms/spring-microservices-security-realm",
    "sub": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "preferred_username": "amine",
    "email": "amine@example.com",
    "realm_access": {
        "roles": ["ADMIN", "USER"]
    },
    "iat": 1703454000,   // Issued at (Unix timestamp)
    "exp": 1703454300    // Expires at (5 min later)
}

// SIGNATURE
// Created by encrypting Header + Payload with Keycloak's private key
// Validated by microservices using Keycloak's public key
```

### Security: How Signature Works

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          JWT SIGNATURE VERIFICATION                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   KEYCLOAK (Token Creation)                                                     │
│   ─────────────────────────                                                     │
│                                                                                  │
│   Header + Payload ─────► 🔐 PRIVATE KEY ─────► Signature                       │
│                           (kept secret)                                          │
│                                                                                  │
│   ═══════════════════════════════════════════════════════════════════════════   │
│                                                                                  │
│   GATEWAY (Token Verification)                                                  │
│   ────────────────────────────                                                  │
│                                                                                  │
│   Header + Payload ─────► 🔓 PUBLIC KEY ──┐                                     │
│                          (from /certs)    │                                     │
│                                           ▼                                     │
│   Token's Signature ──────────────────► COMPARE                                 │
│                                           │                                     │
│                               ┌───────────┴───────────┐                         │
│                               │                       │                         │
│                            MATCH ✅              MISMATCH ❌                    │
│                               │                       │                         │
│                         Token Valid           Token Tampered                    │
│                                               → Reject (401)                    │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 17. Client Types in Keycloak

### Three Types of Clients

| Type | Use Case | Secret? | Example |
|------|----------|---------|---------|
| **Public** | Browser apps (SPA), Mobile | ❌ No | React, Vue, Angular |
| **Confidential** | Server apps | ✅ Yes | Spring Boot backend |
| **Bearer-only** | API services | N/A | Microservices that only validate tokens |

### Visual Guide

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT TYPES COMPARISON                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   PUBLIC CLIENT                    CONFIDENTIAL CLIENT                          │
│   ─────────────                    ──────────────────                           │
│   ┌──────────────┐                ┌──────────────────┐                          │
│   │  React App   │                │  Backend Server  │                          │
│   │  (Browser)   │                │  (Spring Boot)   │                          │
│   └──────┬───────┘                └────────┬─────────┘                          │
│          │                                  │                                    │
│          │ Can't hide secret               │ Can safely store secret            │
│          │ (JS is visible)                 │ (Server-side)                       │
│          │                                  │                                    │
│          ▼                                  ▼                                    │
│   POST /token                        POST /token                                │
│   client_id=app                      client_id=backend                          │
│   (no secret)                        client_secret=xxx                          │
│                                                                                  │
│   ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│   BEARER-ONLY CLIENT                                                            │
│   ──────────────────                                                            │
│   ┌──────────────────┐                                                          │
│   │   Product Svc    │  ← Never initiates login                                 │
│   │   (Microservice) │  ← Only receives & validates tokens                      │
│   └──────────────────┘  ← Blocks requests without valid JWT                     │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Our Setup

| Component | Client Type | Why |
|-----------|-------------|-----|
| Frontend (React/JS) | **Public** | Browser can't keep secrets |
| Gateway Service | **Bearer-only** | Only validates incoming tokens |
| Microservices | **Bearer-only** | Receive tokens from Gateway |

---

## 18. Advanced Authorization with Policies & Permissions

> 💡 For complex scenarios, Keycloak offers fine-grained authorization beyond simple roles.

### Key Concepts

| Concept | Description | Example |
|---------|-------------|---------|
| **Resource** | What you're protecting | `USER`, `ORDER`, `PRODUCT` |
| **Scope** | Actions on a resource | `CREATE`, `READ`, `UPDATE`, `DELETE` |
| **Policy** | Conditions for access | "Must have ADMIN role" |
| **Permission** | Links Resource + Scope + Policy | "ADMIN can CREATE USER" |

### Example: Fine-Grained Access Control

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    RESOURCE-BASED AUTHORIZATION                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   RESOURCES (Nouns)            SCOPES (Verbs)                                   │
│   ─────────────────            ──────────────                                   │
│   • USER                       • CREATE                                          │
│   • ORDER                      • READ                                            │
│   • PRODUCT                    • UPDATE                                          │
│   • INVOICE                    • DELETE                                          │
│                                                                                  │
│   ═══════════════════════════════════════════════════════════════════════════   │
│                                                                                  │
│   POLICIES (Conditions)                                                         │
│   ─────────────────────                                                         │
│   • Role Policy: User has ADMIN role                                            │
│   • Client Policy: Request from frontend-app                                    │
│   • Time Policy: Between 9 AM - 6 PM                                            │
│   • User Policy: Specific user (e.g., "manager1")                               │
│                                                                                  │
│   ═══════════════════════════════════════════════════════════════════════════   │
│                                                                                  │
│   PERMISSION (Combines All)                                                     │
│   ─────────────────────────                                                     │
│                                                                                  │
│   ┌─────────────────────────────────────────────────────┐                       │
│   │  Permission: "manage-users"                         │                       │
│   │  ─────────────────────────────────────────          │                       │
│   │  Resource: USER                                     │                       │
│   │  Scopes: CREATE, UPDATE, DELETE                     │                       │
│   │  Policy: Role = ADMIN                               │                       │
│   │                                                     │                       │
│   │  → Only users with ADMIN role can create,          │                       │
│   │    update, or delete USER resources                 │                       │
│   └─────────────────────────────────────────────────────┘                       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### RPT (Requesting Party Token)

For advanced authorization, Keycloak uses RPT - a JWT enriched with permission data:

```json
{
    "authorization": {
        "permissions": [
            {
                "rsid": "uuid-1234",
                "rsname": "USER",
                "scopes": ["CREATE", "READ"]
            },
            {
                "rsid": "uuid-5678",
                "rsname": "ORDER",
                "scopes": ["READ"]
            }
        ]
    }
}
```

---

## 19. Production Best Practices

### ⚡ Efficiency

| Practice | Why | How |
|----------|-----|-----|
| **Cache JWK Set** | Avoid fetching public keys on every request | Spring caches automatically |
| **Use short access tokens** | Reduce window of compromise | 5-15 minutes |
| **Validate tokens locally** | Reduce Keycloak load | JWT signature validation |

### 🔒 Admin Security

| Risk | Mitigation |
|------|------------|
| Unauthorized admin access | Use strong passwords, 2FA |
| API exposure | Restrict admin API to internal network |
| Default credentials | Change admin/admin immediately |

### 📈 High Availability

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      KEYCLOAK HIGH AVAILABILITY                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│                        ┌─────────────────┐                                      │
│                        │  Load Balancer  │                                      │
│                        └────────┬────────┘                                      │
│                                 │                                                │
│               ┌─────────────────┼─────────────────┐                             │
│               │                 │                 │                              │
│        ┌──────▼──────┐   ┌──────▼──────┐   ┌──────▼──────┐                      │
│        │  Keycloak   │   │  Keycloak   │   │  Keycloak   │                      │
│        │  Instance 1 │   │  Instance 2 │   │  Instance 3 │                      │
│        └──────┬──────┘   └──────┬──────┘   └──────┬──────┘                      │
│               │                 │                 │                              │
│               └─────────────────┼─────────────────┘                             │
│                                 │                                                │
│                        ┌────────▼────────┐                                      │
│                        │  Shared MySQL   │                                      │
│                        │    Database     │                                      │
│                        └─────────────────┘                                      │
│                                                                                  │
│   ✅ If one instance fails, others continue serving                            │
│   ✅ Database stores all config and sessions                                   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 📦 HTTP Header Size

| Problem | Solution |
|---------|----------|
| Large JWT in headers may be blocked | Keep token payload minimal |
| Too many roles/claims | Use role hierarchies |
| Excessive custom claims | Only include essential data |

---

## 20. HTTP Status Codes Reference

| Code | Name | When It Happens |
|------|------|-----------------|
| **200** | OK | Request successful |
| **201** | Created | Resource created (POST) |
| **400** | Bad Request | Invalid request format |
| **401** | Unauthorized | Missing or invalid JWT token |
| **403** | Forbidden | Valid token but insufficient permissions |
| **404** | Not Found | Resource doesn't exist |
| **500** | Server Error | Internal server issue |

### Handling in Frontend

```javascript
const response = await API.get('/api/products');

switch (response.status) {
    case 200:
        // Success - process data
        break;
    case 401:
        // Token invalid/missing - redirect to login
        Auth.logout();
        break;
    case 403:
        // No permission - show error
        alert('You do not have permission to access this resource');
        break;
    case 500:
        // Server error - show retry option
        alert('Server error. Please try again later.');
        break;
}
```

---

## 🎉 You're Done!

You now have a complete understanding of:
- ✅ Keycloak setup and configuration
- ✅ OAuth2 flows and JWT tokens
- ✅ Spring Boot Gateway as Resource Server
- ✅ Frontend authentication (custom login page)
- ✅ Token refresh strategies
- ✅ Role-based access control
- ✅ Getting current user info