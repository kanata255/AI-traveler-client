# Spec Delta

## Purpose

Provides user registration, login, logout, and current-user retrieval, with session management and brute-force protections.

## ADDED Requirements

### Requirement: User registration
A user SHALL be able to register with a required phone number, a password, and an optional email and nickname. Registration SHALL succeed only when the phone number (and email, if provided) is not already registered. On success the user SHALL be logged in automatically and receive an auth token.

#### Scenario: Register with phone and password
- **WHEN** a user submits a valid phone number and password
- **THEN** the system creates the account, logs the user in, and returns an auth token

#### Scenario: Duplicate phone number
- **WHEN** a user registers with a phone number that is already registered
- **THEN** the system rejects the request with an error

#### Scenario: Missing phone number
- **WHEN** a user registers without a phone number
- **THEN** the system rejects the request with a validation error

### Requirement: User login by phone or email
A user SHALL be able to log in using a phone number or email plus a password. On correct credentials the system SHALL return an auth token.

#### Scenario: Login with phone number
- **WHEN** a user submits a registered phone number and the correct password
- **THEN** the system returns an auth token

#### Scenario: Login with email
- **WHEN** a user submits a registered email and the correct password
- **THEN** the system returns an auth token

#### Scenario: Wrong password
- **WHEN** a user submits a correct account but an incorrect password
- **THEN** the system rejects the request with a generic credential error

#### Scenario: Unknown account
- **WHEN** a user submits a phone number or email that is not registered
- **THEN** the system rejects the request with the same generic credential error

### Requirement: User logout
A logged-in user SHALL be able to log out, invalidating their session.

#### Scenario: Logout
- **WHEN** a logged-in user requests logout
- **THEN** the system invalidates the session and subsequent authenticated requests are rejected

### Requirement: Get current user
A logged-in user SHALL be able to retrieve their own profile without exposing the password.

#### Scenario: Retrieve current user
- **WHEN** a logged-in user requests their profile
- **THEN** the system returns the profile fields, excluding the password

#### Scenario: Not logged in
- **WHEN** an unauthenticated request asks for the current user
- **THEN** the system rejects the request as not logged in

### Requirement: Session expiry
An auth token SHALL expire 2 hours after login, after which the user MUST log in again.

#### Scenario: Token expires after two hours
- **WHEN** a user logs in
- **THEN** the returned token is valid for 2 hours and is rejected afterward

### Requirement: Account lockout on repeated failures
The system SHALL lock an account for 15 minutes after 5 consecutive failed login attempts. A successful login SHALL reset the failure count.

#### Scenario: Lock after five failures
- **WHEN** a user fails login 5 times in a row for the same account
- **THEN** the system rejects further attempts for that account for 15 minutes

#### Scenario: Successful login resets the count
- **WHEN** a user logs in successfully
- **THEN** the system clears any prior failure count for that account

### Requirement: Login rate limiting
The system SHALL limit login attempts by IP to 10 requests per minute.

#### Scenario: Exceed rate limit
- **WHEN** a single IP attempts login more than 10 times within one minute
- **THEN** the system rejects the excess attempts

### Requirement: Password stored as hash
Passwords SHALL be stored using BCrypt hashing and MUST never be stored or returned in plaintext.

#### Scenario: Password not stored in plaintext
- **WHEN** an account is created
- **THEN** the stored password value is a BCrypt hash, not the plaintext password