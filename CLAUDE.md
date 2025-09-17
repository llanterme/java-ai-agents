# Java AI Agents System

A comprehensive Java-based agentic system that orchestrates multiple AI agents for content generation, research, and image creation using OpenAI APIs.

## Architecture Overview

This system implements a sequential agent workflow using **LangChain4j** and **Spring Boot 3+**:

```
User Request → Research Agent (with Web Search) → Content Agent → Image Agent → Response
```

### Core Components

1. **Research Agent** (`ResearchAgent.java`): 
   - Generates 5-7 bullet points of research insights
   - **NEW**: Integrates real-time web search via SERP API
   - Combines current web data with LLM knowledge
   - Provides source attribution for facts
2. **Content Agent** (`ContentAgent.java`): Creates platform-specific content (Twitter, LinkedIn, Instagram, Blog)
3. **Image Agent** (`ImageAgent.java`): Generates image briefs and creates images using DALL-E
4. **Agent Graph** (`AgentGraph.java`): Orchestrates the sequential workflow with error handling
5. **Web Search Service** (`SerpApiSearchService.java`): 
   - **NEW**: Performs real-time Google searches
   - Caches results to reduce API costs
   - Extracts organic results and knowledge graphs

## Technology Stack

- **Java 17+**
- **Spring Boot 3.2.0** - Web framework and dependency injection
- **LangChain4j 0.27.1** - LLM integration and prompt management
- **OpenAI APIs** - GPT-4o for text, DALL-E-3 for images
- **SERP API** - Real-time web search integration
- **MySQL 8.x** - Database for user management and authentication
- **Spring Security 6** - JWT-based stateless authentication
- **Flyway** - Database migration management
- **BCrypt** - Password hashing and verification
- **JWT (JJWT)** - Token generation and validation
- **Caffeine Cache** - High-performance caching for search results
- **OkHttp** - HTTP client for external APIs
- **Maven** - Build and dependency management
- **JUnit 5** - Testing framework with comprehensive mocks

## Project Structure

```
src/main/java/za/co/digitalcowboy/agents/
├── agents/              # Core AI agents
├── api/                 # REST API endpoints
├── config/              # Spring configuration
├── domain/              # Domain models and DTOs
│   ├── auth/           # Authentication DTOs
│   ├── oauth/          # OAuth entities and DTOs
│   └── social/         # Social media DTOs
├── graph/               # Agent orchestration
├── repository/          # JPA repositories
├── security/            # JWT and security components
├── service/             # Business logic services
│   ├── oauth/          # OAuth providers and services
│   └── social/         # Social media posting services
└── tools/               # External service integrations

src/main/resources/
├── db/migration/        # Flyway database migrations
└── application.yml      # Application configuration

src/test/java/
├── za/co/digitalcowboy/agents/
│   ├── api/            # Controller integration tests
│   ├── security/       # Security component tests
│   └── service/        # Service unit tests
└── resources/
    └── application-test.yml  # Test configuration
```

## Key Features

### JWT Authentication & Security
- **Stateless Authentication**: JWT-based authentication with access and refresh tokens
- **User Management**: Complete user registration and login system with MySQL persistence
- **Password Security**: BCrypt hashing with configurable strength (default: 12)
- **Token Management**: 30-minute access tokens, 7-day refresh tokens
- **Role-Based Access**: Extensible role system with Spring Security integration
- **Protected Endpoints**: All generation endpoints require valid authentication
- **Database Migrations**: Flyway-managed schema with version control

### Real-Time Web Search Integration
- **SERP API Integration**: Google search for current information
- **Hybrid Research**: Combines web search results with LLM knowledge
- **Smart Query Generation**: LLM generates optimized search queries
- **Source Attribution**: Links to original sources for verification
- **Result Caching**: 1-hour cache to reduce API costs

### Platform-Specific Content Generation
- **Twitter**: ≤280 characters, hashtags, casual tone
- **LinkedIn**: 3-5 paragraphs, professional networking
- **Instagram**: Visual focus, hashtags, engaging captions
- **Blog**: 300-500 words, structured with headlines

### Image Management
- **Local Download**: Configurable image storage with `ImageDownloadService`
- **HTTP Serving**: Static resource handler serves images at `/generated-image/**`
- **Configurable URLs**: Uses `images.base-url` for deployment flexibility
- **Multiple Formats**: Returns OpenAI URLs, local paths, and HTTP URLs

### LinkedIn OAuth & Social Posting
- **OAuth 2.0 Integration**: Complete LinkedIn OAuth flow with secure token storage
- **Token Encryption**: AES-256 encryption for access tokens in database
- **Automatic Token Management**: 60-day token expiration tracking
- **Content Posting**: Direct posting to LinkedIn with text and images
- **Connection Management**: List, connect, and disconnect OAuth providers
- **Scope Validation**: Ensures proper permissions (w_member_social) for posting

### Error Handling & Resilience
- **Graceful Degradation**: Failed agents don't break the workflow
- **Fallback Responses**: Default content when APIs fail
- **Web Search Fallback**: Continues with LLM-only if search fails
- **Comprehensive Logging**: Structured logging with SLF4J
- **Metrics & Observability**: Micrometer timers for performance tracking

## Configuration

### Required Environment Variables

```bash
# OpenAI Configuration
OPENAI_API_KEY=your_openai_api_key_here

# SERP API Configuration (for Web Search)
SERPAPI_KEY=your_serpapi_key_here          # Required for web search

# Database Configuration (Required for Authentication)
DB_HOST=localhost                          # Default: localhost
DB_PORT=3306                              # Default: 3306
DB_NAME=java_ai_agents                    # Default: java_ai_agents
DB_USERNAME=root                          # Default: root
DB_PASSWORD=your_mysql_password           # Required

# JWT Configuration (Required for Authentication)
JWT_SECRET=your_base64_encoded_secret     # Required: base64 encoded secret (min 256 bits)
JWT_ACCESS_TOKEN_EXPIRY_MINUTES=30        # Default: 30 minutes
JWT_REFRESH_TOKEN_EXPIRY_DAYS=7           # Default: 7 days

# LinkedIn OAuth Configuration
LINKEDIN_CLIENT_ID=your_linkedin_client_id  # Required for OAuth
LINKEDIN_CLIENT_SECRET=your_linkedin_secret # Required for OAuth
OAUTH_REDIRECT_BASE_URL=http://localhost:8080 # Base URL for OAuth callbacks
TOKEN_ENCRYPTION_KEY=your_aes_256_key      # Required: 32-byte key for token encryption

# Optional Configuration
OPENAI_TEXT_MODEL=gpt-4o                    # Default: gpt-4o
OPENAI_IMAGE_MODEL=dall-e-3                 # Default: dall-e-3
OPENAI_TIMEOUT_MS=30000                     # Default: 30000 (text generation)
OPENAI_IMAGE_TIMEOUT_MS=120000              # Default: 120000 (image generation)

# Web Search Configuration
SERPAPI_ENGINE=google                       # Default: google
SERPAPI_LOCATION=United States              # Default: United States
SERPAPI_MAX_RESULTS=5                       # Default: 5
SERPAPI_ENABLED=true                        # Default: true

# Image Management
IMAGES_DOWNLOAD_ENABLED=true               # Default: true
IMAGES_STORAGE_PATH=./generated-images     # Default: ./generated-images
IMAGES_KEEP_REMOTE_URL=false               # Default: false
IMAGES_BASE_URL=http://localhost:8080      # Default: localhost:8080
```

### Application Properties

Key configuration in `application.yml`:

```yaml
openai:
  api-key: ${OPENAI_API_KEY:}
  text-model: ${OPENAI_TEXT_MODEL:gpt-4o}
  image-model: ${OPENAI_IMAGE_MODEL:dall-e-3}
  timeout-ms: ${OPENAI_TIMEOUT_MS:30000}
  image-timeout-ms: ${OPENAI_IMAGE_TIMEOUT_MS:120000}
  temperature: 0.1
  max-tokens: 2000

images:
  download-enabled: ${IMAGES_DOWNLOAD_ENABLED:true}
  local-storage-path: ${IMAGES_STORAGE_PATH:./generated-images}
  keep-remote-url: ${IMAGES_KEEP_REMOTE_URL:false}
  base-url: ${IMAGES_BASE_URL:http://localhost:8080}
```

## API Documentation

### Authentication Endpoints

#### POST /api/v1/auth/register

Registers a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "name": "John",
  "surname": "Doe",
  "password": "securePassword123"
}
```

**Validation Rules:**
- `email`: Required, valid email format, unique
- `name`: Required, max 100 characters
- `surname`: Required, max 100 characters  
- `password`: Required, minimum 8 characters

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 1800
}
```

#### POST /api/v1/auth/login

Authenticates a user and returns tokens.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 1800
}
```

#### POST /api/v1/auth/refresh

Refreshes access token using refresh token.

**Request Headers:**
```
Authorization: Bearer <refresh_token>
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 1800
}
```

#### GET /api/v1/auth/me

Returns the current authenticated user's profile. **Requires Authentication**

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "John",
  "surname": "Doe",
  "roles": ["USER"],
  "active": true,
  "createdAt": "2025-01-15T10:30:00Z"
}
```

### Content Generation Endpoints (Protected)

**Note:** All generation endpoints require authentication via `Authorization: Bearer <access_token>` header.

### POST /api/v1/generate

Generates content using the agent workflow (synchronous). **Requires Authentication**

**Request Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "topic": "Artificial Intelligence in Healthcare",
  "platform": "linkedin",
  "tone": "professional", 
  "imageCount": 2
}
```

**Validation Rules:**
- `topic`: Required, 1-200 characters
- `platform`: Required, one of: twitter, linkedin, instagram, blog
- `tone`: Required, one of: professional, casual, authoritative, friendly, technical
- `imageCount`: Optional, 0-4 (default: 1)

**Response:**
```json
{
  "research": {
    "topic": "AI in Healthcare",
    "insights": ["insight1", "insight2", ...]
  },
  "content": {
    "platform": "linkedin",
    "tone": "professional",
    "headline": "Generated headline",
    "body": "Generated content body...",
    "cta": "Call to action",
    "hashtags": ["#AI", "#Healthcare"]
  },
  "image": {
    "prompt": "Generated image prompt",
    "openAiImageUrls": ["https://oaidalleapi..."],
    "localImagePaths": ["/absolute/path/to/image.png"],
    "localImageUrls": ["http://localhost:8080/generated-image/filename.png"]
  }
}
```

### POST /api/v1/generate/async

Starts asynchronous content generation and returns immediately with a task ID. **Requires Authentication**

**Request Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request Body:**
Same as synchronous endpoint.

**Response:**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING", 
  "statusUrl": "/api/v1/generate/status/550e8400-e29b-41d4-a716-446655440000",
  "resultUrl": "/api/v1/generate/result/550e8400-e29b-41d4-a716-446655440000"
}
```

### GET /api/v1/generate/status/{taskId}

Returns the current status of an async generation task. **Requires Authentication**

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "IN_PROGRESS",
  "createdAt": "2025-09-04T08:15:30Z",
  "completedAt": null,
  "error": null
}
```

**Status Values:**
- `PENDING`: Task queued but not started
- `IN_PROGRESS`: Task currently executing 
- `COMPLETED`: Task finished successfully
- `FAILED`: Task failed with error

### POST /api/v1/connections/{provider}/connect

Initiates OAuth connection for a social platform. **Requires Authentication**

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Query Parameters:**
- `redirect_uri` (optional): Custom redirect URI after OAuth completion

**Response (200 OK):**
```json
{
  "authorizationUrl": "https://www.linkedin.com/oauth/v2/authorization?...",
  "provider": "linkedin"
}
```

### GET /api/v1/connections/{provider}/callback

Handles OAuth callback from provider (automatically called by OAuth provider).

**Query Parameters:**
- `code`: Authorization code from OAuth provider
- `state`: State parameter for CSRF protection

**Response:**
Redirects to frontend success/error page

### GET /api/v1/connections

Lists all active OAuth connections for the authenticated user. **Requires Authentication**

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
[
  {
    "provider": "linkedin",
    "providerUserId": "Li_xxxx",
    "connectedAt": "2025-01-15T10:30:00Z",
    "expiresAt": "2025-03-15T10:30:00Z",
    "active": true,
    "scopes": ["openid", "profile", "email", "w_member_social"]
  }
]
```

### DELETE /api/v1/connections/{provider}

Disconnects an OAuth provider. **Requires Authentication**

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "message": "Successfully disconnected from linkedin",
  "provider": "linkedin"
}
```

### POST /api/v1/social/linkedin/post

Posts content to LinkedIn. **Requires Authentication and Active LinkedIn Connection**

**Request Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "text": "Your post content here",
  "imagePath": "/path/to/local/image.jpg"  // Optional
}
```

**Response (200 OK):**
```json
{
  "postId": "urn:li:share:7239583047289372673",
  "state": "PUBLISHED",
  "postUrl": "https://www.linkedin.com/feed/update/urn:li:share:7239583047289372673",
  "message": "Post created successfully via OAuth"
}
```

### GET /api/v1/social/linkedin/status

Checks LinkedIn connection status. **Requires Authentication**

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "connected": true,
  "message": "LinkedIn connection is active and ready for posting"
}
```

### GET /api/v1/generate/result/{taskId}

Returns the result of a completed async generation task. **Requires Authentication**

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
Same format as synchronous endpoint when status is `COMPLETED`, or 404 if not ready.

## Async Task Management

### Architecture
- **In-Memory Storage**: Tasks stored in `ConcurrentHashMap` (not Redis)
- **Thread Pool**: Configurable async executor with 5-20 threads
- **Auto-Cleanup**: Tasks removed after 1 hour
- **Graceful Shutdown**: Waits up to 30 seconds for task completion

### Configuration
```yaml
# Async thread pool configuration in AsyncConfig.java
core-pool-size: 5
max-pool-size: 20  
queue-capacity: 100
thread-name-prefix: "AsyncGeneration-"
```

### Performance Notes
- **Fixed Issue**: Resolved Spring @Async self-invocation problem using `CompletableFuture.runAsync()`
- **Response Time**: Async endpoint returns in ~50ms vs 20+ seconds for sync
- **Memory**: Tasks consume memory until cleanup - monitor in production
- **Persistence**: Tasks lost on application restart (use Redis/DB for production)

## Development Setup

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.x (running locally or remote)
- OpenAI API key
- SERP API key (optional, for web search)

### Quick Start

1. **Clone and setup:**
```bash
git clone <repository>
cd java-ai-agents
```

2. **Setup MySQL database:**
```sql
CREATE DATABASE java_ai_agents;
CREATE USER 'ai_user'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON java_ai_agents.* TO 'ai_user'@'localhost';
FLUSH PRIVILEGES;
```

3. **Generate JWT secret:**
```bash
# Generate a secure 256-bit base64 encoded secret
openssl rand -base64 32
```

4. **Set environment variables:**
```bash
export OPENAI_API_KEY=your_openai_key_here
export DB_PASSWORD=secure_password
export DB_USERNAME=ai_user
export JWT_SECRET=your_base64_encoded_secret_from_step_3
export SERPAPI_KEY=your_serpapi_key_here  # Optional
```

5. **Build and run:**
```bash
mvn clean compile
mvn spring-boot:run
```

6. **Register a user and test the API:**
```bash
# First, register a user account
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "name": "Test",
    "surname": "User",
    "password": "securePassword123"
  }'

# Extract the access token from the response and use it for authenticated requests
export ACCESS_TOKEN="your_access_token_from_registration_response"

# Connect LinkedIn account (opens authorization URL in browser)
curl -X POST http://localhost:8080/api/v1/connections/linkedin/connect \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq -r '.authorizationUrl'
# Open the returned URL in browser to authorize LinkedIn access

# Check LinkedIn connection status
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/social/linkedin/status

# Synchronous generation (20+ seconds) - now requires authentication
curl -X POST http://localhost:8080/api/v1/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "topic": "Machine Learning",
    "platform": "twitter", 
    "tone": "casual",
    "imageCount": 1
  }'

# Async generation (returns immediately) - now requires authentication
curl -X POST http://localhost:8080/api/v1/generate/async \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "topic": "Machine Learning",
    "platform": "twitter", 
    "tone": "casual",
    "imageCount": 1
  }'

# Check async task status - requires authentication
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/generate/status/TASK_ID

# Get async result (when completed) - requires authentication
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/generate/result/TASK_ID

# Get current user profile
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/auth/me

# Generate content and post to LinkedIn (complete workflow)
# Step 1: Generate content
RESPONSE=$(curl -X POST http://localhost:8080/api/v1/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "topic": "AI Innovation",
    "platform": "linkedin",
    "tone": "professional",
    "imageCount": 1
  }')

# Step 2: Extract generated text and image
TEXT=$(echo $RESPONSE | jq -r '.content.body')
IMAGE_PATH=$(echo $RESPONSE | jq -r '.image.localImagePaths[0]')

# Step 3: Post to LinkedIn
curl -X POST http://localhost:8080/api/v1/social/linkedin/post \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{
    \"text\": \"$TEXT\",
    \"imagePath\": \"$IMAGE_PATH\"
  }"
```

### Build Commands

```bash
# Clean build
mvn clean compile

# Run tests
mvn test

# Package application
mvn clean package

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Testing

### Test Coverage
- **Unit Tests**: All agents, services, and controllers
- **Integration Tests**: Full workflow testing with mocks
- **Error Scenarios**: API failures, validation errors, edge cases

### Running Tests
```bash
# All tests
mvn test

# Specific test class  
mvn test -Dtest=AgentFlowTests

# With coverage
mvn clean test jacoco:report
```

### Test Configuration
Tests use comprehensive mocking:
- **OpenAI API calls**: Mocked with realistic responses
- **Image download**: Mocked file operations
- **Error scenarios**: Simulated API failures

## Deployment

### Environment-Specific Configuration

**Local Development:**
```bash
IMAGES_BASE_URL=http://localhost:8080
```

**AWS/Production:**
```bash
IMAGES_BASE_URL=https://your-api.amazonaws.com
IMAGES_STORAGE_PATH=/opt/app/images
OPENAI_TIMEOUT_MS=60000
```

### Docker Deployment
```dockerfile
FROM openjdk:17-jre-slim
COPY target/agents-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### Health Checks
- **Actuator endpoint**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Custom health indicators**: OpenAI connectivity

## Recent Changes & Features

### LinkedIn OAuth Integration & Social Posting (Latest)
- **OAuth 2.0 Flow**: Complete LinkedIn OAuth implementation with authorization code flow
- **Secure Token Storage**: AES-256 encryption for access tokens stored in MySQL
- **Token Management**: Automatic expiration tracking (60-day LinkedIn tokens)
- **Content Posting**: Direct posting to LinkedIn using v2 API (ugcPosts endpoint)
- **Image Support**: Upload and attach images to LinkedIn posts
- **Connection Management**: RESTful endpoints for managing OAuth connections
- **Scope Validation**: Ensures w_member_social permission for posting
- **Error Handling**: Graceful fallback and comprehensive error messages
- **Database Schema**: New connected_accounts table with Flyway migrations

### JWT Authentication System
- **Complete Authentication**: Full JWT-based user registration, login, and token refresh system
- **User Management**: MySQL-based user persistence with Flyway database migrations
- **Password Security**: BCrypt hashing with configurable strength (default: strength 12)
- **Token Strategy**: 30-minute access tokens, 7-day refresh tokens with HMAC-SHA256 signing
- **Protected Endpoints**: All generation endpoints now require valid authentication
- **Spring Security 6**: Modern SecurityFilterChain configuration with stateless sessions
- **Comprehensive Testing**: Full test coverage for authentication components and integration
- **Error Handling**: Proper HTTP status codes (400/401/403) with meaningful error messages

### Real-Time Web Search Integration & Timeout Improvements
- **Web Search**: Integrated SERP API for real-time Google searches
- **Hybrid Research**: ResearchAgent combines web search results with LLM knowledge
- **Smart Queries**: Automatic generation of optimized search queries
- **Source Attribution**: Links to original content for verification
- **Caching**: Caffeine cache implementation for 1-hour result caching
- **Graceful Fallback**: LLM-only research if search fails
- **Timeout Fix**: Separate 2-minute timeout for DALL-E image generation
- **Configuration**: OPENAI_IMAGE_TIMEOUT_MS environment variable

### Async Generation System
- Added `/api/v1/generate/async` endpoint for non-blocking generation
- Task status tracking with `/api/v1/generate/status/{taskId}` 
- Result retrieval with `/api/v1/generate/result/{taskId}`
- **Critical Fix**: Resolved Spring @Async self-invocation issue using `CompletableFuture.runAsync()`
- In-memory task storage with automatic cleanup after 1 hour
- Configurable thread pool executor (5-20 threads, 100 queue capacity)
- Graceful shutdown handling for running tasks

### Local Image URLs
- Added `localImageUrls` field to `ImageResult`
- Configurable base URL via `images.base-url`
- Static resource handler at `/generated-image/**`
- Supports deployment to any domain/port

### Package Structure Migration
- Updated from `com.example.agentic` to `za.co.digitalcowboy.agents`
- All imports and configurations updated
- Tests verified and working

### Image Download System
- `ImageDownloadService` with configurable storage
- Local file management with unique naming
- Error handling for download failures

## Troubleshooting

### Common Issues

1. **OpenAI API Key Missing**
```
Error: OpenAI API key not configured
Solution: Set OPENAI_API_KEY environment variable
```

2. **Image Download Failures**  
```
Error: Failed to create storage directory
Solution: Check file permissions and IMAGES_STORAGE_PATH
```

3. **Platform Content Validation**
```
Error: Twitter content exceeds 280 characters  
Solution: System automatically truncates, warning logged
```

4. **Image Generation Timeout Errors**
```
Error: java.net.SocketTimeoutException: timeout (in OpenAiImageTool)
Cause: DALL-E image generation takes 30-120 seconds but timeout is 30s
Solution: Use separate timeout for image generation
Configuration: Set OPENAI_IMAGE_TIMEOUT_MS=120000 (2 minutes)
```

5. **Async Endpoint Taking 20+ Seconds (FIXED)**
```
Issue: /api/v1/generate/async was running synchronously instead of async
Cause: Spring @Async self-invocation problem in AsyncGenerationService
Fix: Replaced @Async with CompletableFuture.runAsync() to avoid proxy bypass
Result: Async endpoint now returns in ~50ms as expected
```

6. **Task Not Found After Restart**
```
Error: Async task returns null after application restart
Cause: In-memory storage (ConcurrentHashMap) loses data on restart
Solution: Use Redis or database for production task persistence
```

### Debug Mode
```bash
# Enable debug logging
export SPRING_PROFILES_ACTIVE=debug
mvn spring-boot:run
```

## Performance Considerations

- **Timeout Configuration**: Adjust `openai.timeout-ms` for production
- **Image Storage**: Monitor `generated-images/` directory size
- **Metrics**: Use `/actuator/metrics` for performance monitoring
- **Error Rates**: Monitor agent failure rates for API reliability

## Security Notes

- **API Keys**: Never commit keys to repository
- **File Paths**: Validate image storage paths 
- **Input Validation**: All requests validated with Bean Validation
- **Error Messages**: Sanitized error responses

## Extensions & Customization

### Adding New Platforms
1. Update `Platform` enum in validation
2. Add platform-specific logic in `ContentAgent`
3. Update content validation rules
4. Add tests for new platform

### Custom Agents
1. Implement agent interface pattern
2. Add to `AgentGraph` orchestration
3. Configure dependencies in Spring
4. Add metrics and error handling

### Image Formats
- Current: PNG default from DALL-E
- Extension: Support JPEG, WebP in `ImageDownloadService`
- Configuration: Add format selection to API

---

## Developer Notes

**Last Updated**: 2025-01-16 (Added LinkedIn OAuth Integration & Social Posting)
**Version**: 2.1.0
**LangChain4j**: 0.27.1
**Spring Boot**: 3.2.0
**JWT (JJWT)**: 0.12.3
**MySQL**: 8.x
**Flyway**: Latest Spring Boot managed version

For questions or contributions, refer to the comprehensive test suite and existing patterns in the codebase.