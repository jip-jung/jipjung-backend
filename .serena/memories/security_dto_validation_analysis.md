# Security Components and DTO Validation Analysis - JipJung Backend

## 1. Security Components Overview

### 1.1 SecurityConfig.java
**Location**: `/src/main/java/com/jipjung/project/config/SecurityConfig.java`

**Key Responsibilities**:
- Configures Spring Security with JWT-based authentication
- Defines HTTP authorization rules for different endpoints
- Sets up filters for JWT validation and custom login handling
- Configures CORS, session management, and password encoding

**Key Methods**:
- `securityFilterChain()` - Main security configuration
- `passwordEncoder()` - Uses DelegatingPasswordEncoder (bcrypt by default)
- `authenticationManager()` - Creates DaoAuthenticationProvider
- `customJsonUsernamePasswordAuthenticationFilter()` - Handles JSON login requests
- `jwtAuthenticationFilter()` - Validates JWT on each request
- `loginSuccessHandler()` - Generates JWT on successful login
- `loginFailureHandler()` - Handles failed login attempts
- `corsConfigurationSource()` - CORS config with wildcard origins

**Authorization Rules**:
- `/api/auth/**` - Public (signup, login)
- `/api/apartments` - Public (read-only)
- `/api/apartments/favorites/**` - Authenticated only
- `/swagger-ui/**`, `/h2-console/**` - Public (dev tools)
- Everything else - Requires authentication

---

## 2. JWT Implementation

### 2.1 JwtProvider.java
**Location**: `/src/main/java/com/jipjung/project/config/jwt/JwtProvider.java`

**Responsibilities**:
- Generates JWT access tokens
- Validates JWT tokens
- Extracts claims (email) from tokens
- Handles token expiration

**Key Methods**:
- `createAccessToken(String email)` - Generates token with email claim
  - Uses HMAC512 algorithm
  - Subject: "AccessToken"
  - Claim: "email"
  - Expiration: configured via `jwt.access-token-expiration`
  
- `validateToken(String token)` - Returns boolean, handles JWTVerificationException
  
- `getEmailFromToken(String token)` - Extracts email claim, returns null on error
  
- `extractToken(String authorizationHeader)` - Strips "Bearer " prefix

**Security Notes**:
- Secret key injected from `${jwt.secret}` property
- Token format: `Bearer <JWT_TOKEN>`
- No refresh token implementation
- Claims only contain email, no user roles/authorities

---

## 3. JWT Filters and Handlers

### 3.1 JwtAuthenticationFilter.java
**Location**: `/src/main/java/com/jipjung/project/config/jwt/filter/JwtAuthenticationFilter.java`

**Extends**: OncePerRequestFilter

**Responsibilities**:
- Validates JWT tokens on each request
- Sets SecurityContext with user details
- Skips filter for `/api/auth/**` paths

**Key Logic**:
1. Bypasses `/api/auth` paths (signup/login endpoints)
2. Extracts token from Authorization header
3. Validates token signature and expiration
4. Loads UserDetails via LoginService
5. Creates UsernamePasswordAuthenticationToken
6. Sets SecurityContextHolder

**Security Notes**:
- Only validates token format and signature
- Does NOT validate token expiration in detail (done in JwtProvider)
- Sets authorities from CustomUserDetails

---

### 3.2 CustomJsonUsernamePasswordAuthenticationFilter.java
**Location**: `/src/main/java/com/jipjung/project/config/jwt/filter/CustomJsonUsernamePasswordAuthenticationFilter.java`

**Extends**: AbstractAuthenticationProcessingFilter

**Responsibilities**:
- Intercepts POST `/api/auth/login` requests
- Parses JSON body (email + password)
- Delegates to AuthenticationManager for credential validation

**Key Logic**:
1. Checks Content-Type is "application/json"
2. Reads request body as JSON
3. Extracts "email" and "password" fields
4. Creates UsernamePasswordAuthenticationToken
5. Calls AuthenticationManager.authenticate()

**Security Notes**:
- Only accepts POST with JSON content type
- Throws AuthenticationServiceException for invalid content-type
- ObjectMapper used for JSON parsing (potential JSON injection vectors)

---

### 3.3 LoginSuccessHandler.java
**Location**: `/src/main/java/com/jipjung/project/config/jwt/handler/LoginSuccessHandler.java`

**Responsibilities**:
- Generates JWT token on successful authentication
- Returns formatted JSON response with token

**Key Logic**:
1. Extracts CustomUserDetails from authentication
2. Creates JWT token via JwtProvider
3. Sets Authorization header to "Bearer <token>"
4. Returns HTTP 200 with LoginResponse body

**Response Format**:
```json
{
  "code": 200,
  "status": "OK",
  "message": "성공",
  "data": {
    "nickname": "홍길동"
  }
}
```

---

### 3.4 LoginFailureHandler.java
**Location**: `/src/main/java/com/jipjung/project/config/jwt/handler/LoginFailureHandler.java`

**Responsibilities**:
- Returns error response on failed authentication

**Response**:
```json
{
  "code": 401,
  "status": "UNAUTHORIZED",
  "message": "로그인 실패: 이메일 또는 비밀번호가 올바르지 않습니다."
}
```

---

## 4. Request DTOs with Validation

### 4.1 LoginRequest.java
**Location**: `/src/main/java/com/jipjung/project/controller/dto/request/LoginRequest.java`

**Fields with Validation**:
```java
@NotBlank(message = "이메일은 필수입니다")
@Email(message = "유효한 이메일 형식이 아닙니다")
String email;

@NotBlank(message = "비밀번호는 필수입니다")
String password;
```

**Test Cases Needed**:
- Empty email
- Invalid email format
- Empty password
- NULL values
- Whitespace-only values

---

### 4.2 SignupRequest.java
**Location**: `/src/main/java/com/jipjung/project/controller/dto/request/SignupRequest.java`

**Fields with Validation**:
```java
@NotBlank(message = "이메일은 필수입니다")
@Email(message = "유효한 이메일 형식이 아닙니다")
String email;

@NotBlank(message = "닉네임은 필수입니다")
@Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다")
String nickname;

@NotBlank(message = "비밀번호는 필수입니다")
@Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
    message = "비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다"
)
String password;
```

**Test Cases Needed**:
- All required fields missing
- Email validation (empty, invalid format, valid)
- Nickname length boundaries (1, 2, 20, 21 chars)
- Password regex validation:
  - No uppercase (should fail)
  - No digit (should fail)
  - No special char (should fail)
  - 7 chars with all requirements (should fail)
  - 8+ chars with all requirements (should pass)
  - Valid: "Test1234!@"
- Duplicate email (service-level validation)

---

### 4.3 ApartmentSearchRequest.java
**Location**: `/src/main/java/com/jipjung/project/controller/dto/request/ApartmentSearchRequest.java`

**Fields** (NO Jakarta Validation):
```java
String aptNm;           // nullable
String umdNm;           // nullable
String dealDateFrom;    // nullable, expected YYYY-MM-DD format
String dealDateTo;      // nullable, expected YYYY-MM-DD format
Long minDealAmount;     // nullable
Long maxDealAmount;     // nullable
Integer page;           // default 0
Integer size;           // default 10
```

**Custom Validation in Compact Constructor**:
```java
page = (page != null && page >= 0) ? page : 0;
size = (size != null && size > 0 && size <= 100) ? size : 10;
```

**Test Cases Needed**:
- NULL values for optional fields
- Negative page number
- Zero or negative size
- Size > 100
- Date format validation (currently none - potential bug)
- Numeric range validation for deal amounts

**SECURITY ISSUE**: No validation on dealDateFrom/To format - potential injection

---

### 4.4 FavoriteRequest.java
**Location**: `/src/main/java/com/jipjung/project/controller/dto/request/FavoriteRequest.java`

**Fields with Validation**:
```java
@NotBlank(message = "아파트 코드는 필수입니다")
String aptSeq;  // apartment.apt_seq
```

**Test Cases Needed**:
- NULL aptSeq
- Empty string ""
- Whitespace "   "
- Invalid apartment code (not found)
- Valid apartment code

---

## 5. Custom Exception Handlers

### 5.1 GlobalExceptionHandler.java
**Location**: `/src/main/java/com/jipjung/project/config/exception/GlobalExceptionHandler.java`

**Handled Exceptions**:

1. **MethodArgumentNotValidException** (400)
   - Triggered by @Valid annotation violations
   - Returns field-level errors as Map<String, String>
   - Example:
     ```json
     {
       "code": 400,
       "status": "BAD_REQUEST",
       "message": "입력 값 검증 실패",
       "data": {
         "email": "유효한 이메일 형식이 아닙니다",
         "password": "비밀번호는 필수입니다"
       }
     }
     ```

2. **DuplicateEmailException** (409 CONFLICT)
   - Thrown by AuthService.validateSameEmail()
   - Response: ErrorCode.DUPLICATE_EMAIL

3. **ResourceNotFoundException** (404)
   - Thrown when entity not found
   - Contains ErrorCode enum
   - ErrorCodes: USER_NOT_FOUND, APARTMENT_NOT_FOUND, FAVORITE_NOT_FOUND

4. **DuplicateResourceException** (409)
   - Thrown on duplicate resource creation
   - ErrorCodes: DUPLICATE_FAVORITE

5. **IllegalArgumentException** (400)
   - Generic business logic errors
   - Message passed to response

6. **Exception** (500)
   - Catch-all for unexpected errors
   - Returns ErrorCode.INTERNAL_SERVER_ERROR

---

### 5.2 Custom Exception Classes

**DuplicateEmailException**:
```java
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) { super(message); }
}
```

**ResourceNotFoundException**:
```java
public class ResourceNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;
    // Constructor with ErrorCode and optional custom message
    public ErrorCode getErrorCode() { return errorCode; }
}
```

**DuplicateResourceException**:
```java
public class DuplicateResourceException extends RuntimeException {
    private final ErrorCode errorCode;
    // Constructor with ErrorCode and optional custom message
    public ErrorCode getErrorCode() { return errorCode; }
}
```

---

### 5.3 ErrorCode Enum
**Location**: `/src/main/java/com/jipjung/project/config/exception/ErrorCode.java`

**Defined Error Codes**:
```
400 - INVALID_INPUT_VALUE, INVALID_TYPE_VALUE
401 - UNAUTHORIZED, INVALID_AUTH_TOKEN, EXPIRED_AUTH_TOKEN
403 - FORBIDDEN
404 - USER_NOT_FOUND, APARTMENT_NOT_FOUND, FAVORITE_NOT_FOUND
409 - DUPLICATE_EMAIL, DUPLICATE_FAVORITE
500 - INTERNAL_SERVER_ERROR
```

---

### 5.4 ApiResponse.java
**Location**: `/src/main/java/com/jipjung/project/config/exception/ApiResponse.java`

**Record Structure**:
```java
public record ApiResponse<T>(
    int code,              // HTTP status code
    String status,         // HTTP status name
    String message,        // User message
    T data                 // Response data
)
```

**Factory Methods**:
- `successBody(T data)` - Success response body only
- `errorBody(HttpStatus, String)` - Error response body
- `success(T data)` - ResponseEntity with 200 status
- `created(T data)` - ResponseEntity with 201 status
- `error(ErrorCode)` - ResponseEntity with error code

---

## 6. Authentication Service

### 6.1 AuthService.java
**Location**: `/src/main/java/com/jipjung/project/service/AuthService.java`

**Key Methods**:
- `signup(SignupRequest)` - Validates email, creates user, encodes password
- `validateSameEmail(SignupRequest)` - Throws DuplicateEmailException if exists
- `createUser(SignupRequest)` - Builds User with encoded password, default USER role

**Validation Flow**:
1. Request arrives with @Valid annotation
2. GlobalExceptionHandler catches MethodArgumentNotValidException
3. If validation passes, signup() executes
4. validateSameEmail() performs database check
5. DuplicateEmailException thrown if duplicate
6. Password encoded with PasswordEncoder (delegating)
7. User persisted to database

**Test Cases Needed**:
- Valid signup flow
- Duplicate email detection
- Password encoding verification
- Transaction rollback on exception

---

### 6.2 LoginService.java
**Location**: `/src/main/java/com/jipjung/project/service/LoginService.java`

**Implements**: UserDetailsService

**Key Method**:
- `loadUserByUsername(String email)` - Loads user by email, throws UsernameNotFoundException

**Role in Authentication**:
1. Called by CustomJsonUsernamePasswordAuthenticationFilter
2. Called by JwtAuthenticationFilter
3. Returns CustomUserDetails wrapper

**Test Cases Needed**:
- Existing user loads successfully
- Non-existent user throws UsernameNotFoundException

---

### 6.3 CustomUserDetails.java
**Location**: `/src/main/java/com/jipjung/project/service/CustomUserDetails.java`

**Implements**: UserDetails

**Key Methods**:
- `getAuthorities()` - Returns ROLE_<UserRole>
- `getPassword()` - Returns encoded password
- `getUsername()` - Returns email
- `isAccountNonExpired()` - Always true
- `isAccountNonLocked()` - Always true
- `isCredentialsNonExpired()` - Always true
- `isEnabled()` - Always true
- `getEmail()`, `getNickname()`, `getId()` - Custom accessors

**Security Notes**:
- All account status checks return true (no suspension/lock logic)
- No rotation for password expiration
- Role retrieved from User entity

---

## 7. Security-Critical Logic Needing Tests

### 7.1 JWT Token Handling
**Test Scenarios**:
- Valid token with email claim
- Expired token (set expiration to 0ms)
- Invalid signature (modify token)
- Missing Authorization header
- Malformed Authorization header
- Token without email claim
- Null token

**Edge Cases**:
- Very long JWT (DoS)
- Empty token string
- Token with special characters

---

### 7.2 Authentication Flow
**Test Scenarios**:
- Successful login → token issued
- Wrong password → 401 Unauthorized
- Non-existent email → 401 Unauthorized
- Unauthenticated access to protected endpoint → 401
- Valid token on protected endpoint → 200

---

### 7.3 DTO Validation
**Test Scenarios**:
- All validators in SignupRequest/LoginRequest
- Boundary testing for nickname (1, 2, 20, 21)
- Password regex edge cases
- Date format validation (missing validation!)
- Numeric range validation for search

---

### 7.4 Password Security
**Test Scenarios**:
- Password encoding works (not plaintext stored)
- Same password produces different hashes (salt)
- Password verification matches original
- Encoded password never logged
- Default User role assigned

---

### 7.5 CORS and CSRF
**Current Config**:
- CORS allows all origins (`*`)
- CSRF disabled (safe for JWT)
- Credentials false for CORS

**Test Scenarios**:
- OPTIONS request succeeds
- Authorization header exposed in CORS response
- Cross-origin requests accepted

---

## 8. Known Issues and Testing Gaps

### Critical Issues
1. **Date Format Validation Missing**: ApartmentSearchRequest has no validation for YYYY-MM-DD format
2. **CORS Wildcard**: `setAllowedOrigins(Arrays.asList("*"))` accepts all origins (may be intended)
3. **No Token Refresh**: JWT has single expiration, no refresh token mechanism
4. **Email as Primary Key**: User identity depends entirely on email in JWT
5. **No Password Expiration**: CustomUserDetails.isCredentialsNonExpired() always returns true
6. **No Account Lockout**: No mechanism to prevent brute force attacks

### Testing Gaps
1. No test coverage for security config (unit tests missing)
2. No integration tests for authentication flow
3. No tests for filter chain order and interaction
4. No tests for exception handler error messages
5. No performance tests for JWT validation
6. No tests for concurrent authentication requests
7. No tests for token theft/replay scenarios

---

## 9. Summary Table: Components & Responsibilities

| Component | Type | Purpose | Security Focus |
|-----------|------|---------|-----------------|
| SecurityConfig | Config | Defines auth rules & filters | URL access, filter chain |
| JwtProvider | Component | JWT generation/validation | Token security |
| JwtAuthenticationFilter | Filter | Validates token on requests | Authorization |
| CustomJsonUsernamePasswordAuthenticationFilter | Filter | Parses JSON login | Input validation |
| LoginSuccessHandler | Handler | Issues JWT on success | Token response |
| LoginFailureHandler | Handler | Error on failed login | Error handling |
| AuthService | Service | User registration | Duplicate check |
| LoginService | Service | User lookup for auth | UserDetails loading |
| CustomUserDetails | UserDetails | Spring Security user wrapper | Authority/role mapping |
| GlobalExceptionHandler | Advice | Centralized error handling | Error responses |
| ApiResponse | Record | Standard response wrapper | Response format |

