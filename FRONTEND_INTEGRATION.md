# Frontend Integration Guide - Java AI Agents API

This comprehensive guide provides all the information needed to integrate with the Java AI Agents backend API. The system provides AI-powered content generation with authentication, async task management, and multi-platform content creation.

## API Overview

**Base URL**: `http://localhost:8080` (development)  
**API Version**: v1  
**Authentication**: JWT Bearer tokens  
**Content-Type**: `application/json`

### Core Features

- **JWT Authentication**: Stateless token-based authentication with access/refresh tokens
- **AI Content Generation**: Multi-platform content generation (Twitter, LinkedIn, Instagram, Blog)
- **Async Processing**: Non-blocking content generation with task tracking
- **Image Generation**: AI-powered image creation using DALL-E
- **Web Research**: Real-time web search integration for current information

---

## Authentication System

### Overview
The API uses JWT-based authentication with two types of tokens:
- **Access Token**: Short-lived (30 minutes), used for API requests
- **Refresh Token**: Long-lived (7 days), used to get new access tokens

### Authentication Flow
1. Register or login to get initial tokens
2. Include access token in `Authorization: Bearer <token>` header
3. When access token expires, use refresh token to get new tokens
4. All generation endpoints require valid authentication

---

## API Endpoints

### 1. Authentication Endpoints

#### POST /api/v1/auth/register
Register a new user account.

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
- `email`: Required, valid email format, must be unique
- `name`: Required, max 100 characters
- `surname`: Required, max 100 characters
- `password`: Required, minimum 8 characters

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 1800
}
```

**Error Response (400 Bad Request):**
```json
{
  "message": "User with this email already exists",
  "status": 400,
  "timestamp": "2025-01-16T10:30:00",
  "details": null
}
```

**Validation Error Response (400 Bad Request):**
```json
{
  "message": "Validation failed",
  "status": 400,
  "timestamp": "2025-01-16T10:30:00",
  "details": {
    "email": "Invalid email format",
    "password": "Password must be at least 8 characters long"
  }
}
```

---

#### POST /api/v1/auth/login
Authenticate user and return tokens.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Validation Rules:**
- `email`: Required, valid email format
- `password`: Required

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 1800
}
```

**Error Response (401 Unauthorized):**
```json
{
  "message": "Invalid email or password",
  "status": 401,
  "timestamp": "2025-01-16T10:30:00",
  "details": null
}
```

---

#### POST /api/v1/auth/refresh
Refresh access token using refresh token.

**Request Headers:**
```
Authorization: Bearer <refresh_token>
```

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 1800
}
```

**Error Response (400 Bad Request):**
```json
{
  "message": "Missing or invalid refresh token",
  "status": 400,
  "timestamp": "2025-01-16T10:30:00",
  "details": null
}
```

---

#### GET /api/v1/auth/me
Get current authenticated user's profile.

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Success Response (200 OK):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "John",
  "surname": "Doe",
  "roles": ["USER"],
  "active": true,
  "createdAt": "2025-01-15T10:30:00"
}
```

**Error Response (401 Unauthorized):**
```json
{
  "message": "Authentication required",
  "status": 401,
  "timestamp": "2025-01-16T10:30:00",
  "details": null
}
```

---

### 2. Content Generation Endpoints

**⚠️ All generation endpoints require authentication via `Authorization: Bearer <access_token>` header.**

#### POST /api/v1/generate/async
Start asynchronous content generation (recommended for production).

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
- `platform`: Required, one of: `twitter`, `linkedin`, `instagram`, `blog`
- `tone`: Required, one of: `professional`, `casual`, `playful`, `authoritative`
- `imageCount`: Optional, 1-4 (default: 1)

**Success Response (202 Accepted):**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "statusUrl": "/api/v1/generate/status/550e8400-e29b-41d4-a716-446655440000",
  "resultUrl": "/api/v1/generate/result/550e8400-e29b-41d4-a716-446655440000"
}
```

**Error Response (400 Bad Request):**
```json
{
  "message": "Validation failed",
  "status": 400,
  "timestamp": "2025-01-16T10:30:00",
  "details": {
    "platform": "Platform must be one of: twitter, linkedin, instagram, blog"
  }
}
```

---

#### GET /api/v1/generate/status/{taskId}
Check the status of an async generation task.

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Success Response (200 OK):**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "IN_PROGRESS",
  "createdAt": "2025-01-16T10:30:00",
  "completedAt": null,
  "error": null,
  "result": null
}
```

**Task Status Values:**
- `PENDING`: Task queued but not started
- `IN_PROGRESS`: Task currently executing
- `COMPLETED`: Task finished successfully
- `FAILED`: Task failed with error

**Completed Task Response:**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "createdAt": "2025-01-16T10:30:00",
  "completedAt": "2025-01-16T10:32:00",
  "error": null,
  "result": {
    // Full OrchestrationResult object (see result endpoint)
  }
}
```

**Failed Task Response:**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "FAILED",
  "createdAt": "2025-01-16T10:30:00",
  "completedAt": "2025-01-16T10:31:30",
  "error": "OpenAI API request failed: Rate limit exceeded",
  "result": null
}
```

**Error Response (404 Not Found):**
```json
{
  "message": "Resource not found",
  "status": 404,
  "timestamp": "2025-01-16T10:30:00",
  "details": null
}
```

---

#### GET /api/v1/generate/result/{taskId}
Get the result of a completed async generation task.

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Success Response (200 OK):**
```json
{
  "topic": "Artificial Intelligence in Healthcare",
  "research": {
    "topic": "AI in Healthcare",
    "insights": [
      "AI diagnostics can improve accuracy by up to 95% in medical imaging",
      "Machine learning algorithms help predict patient outcomes more effectively",
      "AI-powered drug discovery reduces development time by 30-50%",
      "Robotic surgery enhances precision and reduces recovery time",
      "Natural language processing streamlines clinical documentation"
    ],
    "sources": [
      "https://www.nature.com/articles/s41598-023-12345",
      "https://www.nejm.org/doi/full/10.1056/NEJMra2312345"
    ]
  },
  "content": {
    "platform": "linkedin",
    "tone": "professional",
    "headline": "The Revolutionary Impact of AI in Healthcare",
    "body": "Artificial Intelligence is transforming healthcare delivery across multiple dimensions...\n\nKey breakthroughs include:\n• 95% accuracy improvement in medical imaging\n• 30-50% faster drug discovery processes\n• Enhanced surgical precision through robotics\n\nAs we advance into this new era, the integration of AI technologies promises to deliver more personalized, efficient, and accessible healthcare solutions for patients worldwide.",
    "cta": "What's your take on AI's role in healthcare transformation?",
    "hashtags": ["#AIHealthcare", "#MedicalInnovation", "#DigitalHealth", "#MachineLearning"]
  },
  "image": {
    "prompt": "A modern hospital setting with AI technology, showing doctors using advanced diagnostic tools, digital interfaces, and robotic systems, professional medical environment, clean and futuristic aesthetic",
    "openAiImageUrls": [
      "https://oaidalleapiprodscus.blob.core.windows.net/private/org-123/user-456/img-abc123.png",
      "https://oaidalleapiprodscus.blob.core.windows.net/private/org-123/user-456/img-def456.png"
    ],
    "localImagePaths": [
      "/Users/app/generated-images/ai-healthcare-20250116-103000-001.png",
      "/Users/app/generated-images/ai-healthcare-20250116-103000-002.png"
    ],
    "localImageUrls": [
      "http://localhost:8080/generated-image/ai-healthcare-20250116-103000-001.png",
      "http://localhost:8080/generated-image/ai-healthcare-20250116-103000-002.png"
    ]
  }
}
```

**Error Responses:**

Task not found (404 Not Found):
```json
{
  "message": "Resource not found",
  "status": 404,
  "timestamp": "2025-01-16T10:30:00",
  "details": null
}
```

Task not completed (400 Bad Request):
```json
{
  "message": "Task not completed yet",
  "status": 400,
  "timestamp": "2025-01-16T10:30:00",
  "details": null
}
```

---

### 3. Health and Monitoring Endpoints

#### GET /api/v1/health
Get application health status and task metrics.

**Success Response (200 OK):**
```json
{
  "status": "healthy",
  "timestamp": 1705398600000,
  "activeTasks": 3,
  "totalTasks": 157
}
```

#### GET /actuator/health
Spring Boot Actuator health endpoint.

**Success Response (200 OK):**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963170816,
        "free": 91943170816,
        "threshold": 10485760
      }
    }
  }
}
```

---

## Frontend Integration Examples

### JavaScript/TypeScript Integration

#### Authentication Service
```typescript
interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

interface User {
  id: number;
  email: string;
  name: string;
  surname: string;
  roles: string[];
  active: boolean;
  createdAt: string;
}

class AuthService {
  private baseUrl = 'http://localhost:8080/api/v1';
  private accessToken: string | null = null;
  private refreshToken: string | null = null;

  async register(email: string, name: string, surname: string, password: string): Promise<AuthTokens> {
    const response = await fetch(`${this.baseUrl}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, name, surname, password }),
    });

    if (!response.ok) {
      throw new Error(await response.text());
    }

    const tokens: AuthTokens = await response.json();
    this.setTokens(tokens);
    return tokens;
  }

  async login(email: string, password: string): Promise<AuthTokens> {
    const response = await fetch(`${this.baseUrl}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
      throw new Error(await response.text());
    }

    const tokens: AuthTokens = await response.json();
    this.setTokens(tokens);
    return tokens;
  }

  async getCurrentUser(): Promise<User> {
    const response = await this.authenticatedRequest('/auth/me');
    return response.json();
  }

  private async authenticatedRequest(endpoint: string, options: RequestInit = {}): Promise<Response> {
    if (!this.accessToken) {
      throw new Error('No access token available');
    }

    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      ...options,
      headers: {
        'Authorization': `Bearer ${this.accessToken}`,
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });

    // Handle token expiration
    if (response.status === 401 && this.refreshToken) {
      await this.refreshAccessToken();
      return this.authenticatedRequest(endpoint, options);
    }

    return response;
  }

  private async refreshAccessToken(): Promise<void> {
    if (!this.refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await fetch(`${this.baseUrl}/auth/refresh`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.refreshToken}`,
      },
    });

    if (!response.ok) {
      this.clearTokens();
      throw new Error('Token refresh failed');
    }

    const tokens: AuthTokens = await response.json();
    this.setTokens(tokens);
  }

  private setTokens(tokens: AuthTokens): void {
    this.accessToken = tokens.accessToken;
    this.refreshToken = tokens.refreshToken;
    // Store in localStorage for persistence
    localStorage.setItem('accessToken', tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
  }

  private clearTokens(): void {
    this.accessToken = null;
    this.refreshToken = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  }
}
```

#### Content Generation Service
```typescript
interface GenerationRequest {
  topic: string;
  platform: 'twitter' | 'linkedin' | 'instagram' | 'blog';
  tone: 'professional' | 'casual' | 'playful' | 'authoritative';
  imageCount?: number;
}

interface GenerationResult {
  topic: string;
  research: {
    topic: string;
    insights: string[];
    sources?: string[];
  };
  content: {
    platform: string;
    tone: string;
    headline: string;
    body: string;
    cta: string;
    hashtags: string[];
  };
  image: {
    prompt: string;
    openAiImageUrls: string[];
    localImageUrls: string[];
  };
}

interface TaskStatus {
  taskId: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  completedAt?: string;
  error?: string;
  result?: GenerationResult;
}

class GenerationService {
  constructor(private authService: AuthService) {}

  async startGeneration(request: GenerationRequest): Promise<{ taskId: string; statusUrl: string }> {
    const response = await this.authService.authenticatedRequest('/generate/async', {
      method: 'POST',
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(await response.text());
    }

    return response.json();
  }

  async getTaskStatus(taskId: string): Promise<TaskStatus> {
    const response = await this.authService.authenticatedRequest(`/generate/status/${taskId}`);
    
    if (!response.ok) {
      throw new Error(await response.text());
    }

    return response.json();
  }

  async getTaskResult(taskId: string): Promise<GenerationResult> {
    const response = await this.authService.authenticatedRequest(`/generate/result/${taskId}`);
    
    if (!response.ok) {
      throw new Error(await response.text());
    }

    return response.json();
  }

  // Utility method to poll task until completion
  async waitForCompletion(taskId: string, pollInterval = 2000): Promise<GenerationResult> {
    return new Promise((resolve, reject) => {
      const poll = async () => {
        try {
          const status = await this.getTaskStatus(taskId);
          
          if (status.status === 'COMPLETED') {
            resolve(status.result!);
          } else if (status.status === 'FAILED') {
            reject(new Error(status.error || 'Task failed'));
          } else {
            setTimeout(poll, pollInterval);
          }
        } catch (error) {
          reject(error);
        }
      };
      
      poll();
    });
  }
}
```

#### React Integration Example
```jsx
import React, { useState } from 'react';

function ContentGenerator() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleSubmit = async (formData) => {
    setLoading(true);
    setError(null);

    try {
      // Start async generation
      const { taskId } = await generationService.startGeneration({
        topic: formData.topic,
        platform: formData.platform,
        tone: formData.tone,
        imageCount: formData.imageCount || 1,
      });

      // Wait for completion
      const result = await generationService.waitForCompletion(taskId);
      setResult(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {loading && <div>Generating content...</div>}
      {error && <div className="error">Error: {error}</div>}
      {result && (
        <div className="result">
          <h2>{result.content.headline}</h2>
          <p>{result.content.body}</p>
          {result.image.localImageUrls.map((url, index) => (
            <img key={index} src={url} alt={`Generated image ${index + 1}`} />
          ))}
        </div>
      )}
    </div>
  );
}
```

---

## Error Handling

### HTTP Status Codes
- **200 OK**: Successful request
- **202 Accepted**: Async task started successfully
- **400 Bad Request**: Invalid request data or validation errors
- **401 Unauthorized**: Missing or invalid authentication
- **404 Not Found**: Resource not found (task, endpoint)
- **500 Internal Server Error**: Server-side error

### Error Response Format
All error responses follow this consistent format:
```json
{
  "message": "Human-readable error message",
  "status": 400,
  "timestamp": "2025-01-16T10:30:00",
  "details": {
    // Optional: Additional error details for validation errors
    "field": "error message"
  }
}
```

### Common Error Scenarios

1. **Token Expired**: Use refresh token to get new access token
2. **Invalid Platform**: Must be one of: twitter, linkedin, instagram, blog
3. **Task Not Found**: TaskId doesn't exist or has been cleaned up (tasks expire after 1 hour)
4. **Rate Limiting**: Implement exponential backoff for retry logic
5. **Validation Errors**: Check the `details` field for specific field errors

---

## Best Practices

### 1. Token Management
- Store tokens securely (consider secure cookie over localStorage for production)
- Implement automatic token refresh logic
- Clear tokens on logout or refresh failure
- Handle token expiration gracefully

### 2. Async Operations
- Always use the async endpoints for better user experience
- Implement polling with reasonable intervals (2-5 seconds)
- Provide progress indicators to users
- Handle task failures gracefully with retry options

### 3. Error Handling
- Implement global error handling for API responses
- Show user-friendly error messages
- Log detailed errors for debugging
- Implement retry logic for transient failures

### 4. Performance
- Cache user profile data appropriately
- Implement request debouncing for search/input fields
- Use request/response interceptors for common headers
- Consider implementing request caching where appropriate

### 5. Security
- Never expose tokens in URLs or logs
- Use HTTPS in production
- Implement proper CORS headers
- Validate all user inputs on frontend before sending to API

---

## Environment Configuration

### Development
```typescript
const config = {
  apiBaseUrl: 'http://localhost:8080',
  environment: 'development',
  tokenRefreshBuffer: 60000, // Refresh tokens 1 minute before expiry
  taskPollInterval: 2000, // Poll every 2 seconds
};
```

### Production
```typescript
const config = {
  apiBaseUrl: 'https://api.yourdomain.com',
  environment: 'production',
  tokenRefreshBuffer: 300000, // Refresh tokens 5 minutes before expiry
  taskPollInterval: 5000, // Poll every 5 seconds
};
```

---

## Testing

### Testing Authentication
```typescript
// Test registration
const tokens = await authService.register(
  'test@example.com',
  'Test',
  'User',
  'password123'
);
expect(tokens.accessToken).toBeDefined();

// Test login
const loginTokens = await authService.login('test@example.com', 'password123');
expect(loginTokens.accessToken).toBeDefined();

// Test getting current user
const user = await authService.getCurrentUser();
expect(user.email).toBe('test@example.com');
```

### Testing Content Generation
```typescript
// Test async generation
const { taskId } = await generationService.startGeneration({
  topic: 'AI in Healthcare',
  platform: 'linkedin',
  tone: 'professional',
  imageCount: 1,
});

expect(taskId).toBeDefined();

// Test task status
const status = await generationService.getTaskStatus(taskId);
expect(['PENDING', 'IN_PROGRESS', 'COMPLETED']).toContain(status.status);
```

---

This integration guide provides all the necessary information to successfully integrate with the Java AI Agents API. For additional support or questions, please refer to the API documentation or contact the development team.