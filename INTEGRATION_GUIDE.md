# Integration Guide: Content Persistence & LinkedIn Posting

This guide provides comprehensive integration instructions for UI AI agents to work with the new content persistence and LinkedIn posting features.

## Overview of Changes

The system now provides:
1. **Content Persistence** - All generated content is automatically saved with unique IDs
2. **Content Management** - CRUD operations for managing saved content
3. **Decoupled LinkedIn Posting** - Post content using only content IDs
4. **Enhanced Workflows** - Seamless integration between generation and social posting

---

## Authentication Requirements

All endpoints require JWT authentication via `Authorization` header:

```http
Authorization: Bearer <access_token>
```

### Getting Access Token

**Register New User:**
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "name": "John",
  "surname": "Doe",
  "password": "SecurePass123!"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 1800
}
```

**Login Existing User:**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

---

## Content Generation with Persistence

### Generate Content (Asynchronous)

For long-running operations, use async generation:

**Start Generation:**
```http
POST /api/v1/generate/async
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "topic": "Machine Learning Trends",
  "platform": "twitter",
  "tone": "casual",
  "imageCount": 2
}
```

**Response:**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "statusUrl": "/api/v1/generate/status/550e8400-e29b-41d4-a716-446655440000",
  "resultUrl": "/api/v1/generate/result/550e8400-e29b-41d4-a716-446655440000"
}
```

**Check Status:**
```http
GET /api/v1/generate/status/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <access_token>
```

**Status Response:**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "createdAt": "2025-01-16T10:30:00Z",
  "completedAt": "2025-01-16T10:31:15Z",
  "error": null
}
```

**Get Result:**
```http
GET /api/v1/generate/result/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <access_token>
```

**Result Response:** *(Same format as synchronous generation)*

---

## Content Management

### Get All User Content

**Request:**
```http
GET /api/v1/content
Authorization: Bearer <access_token>
```

**Response:**
```json
[
  {
    "id": 125,
    "topic": "Remote Work Productivity",
    "platform": "linkedin",
    "tone": "professional",
    "imageCount": 1,
    "research": { /* research data */ },
    "content": { /* content data */ },
    "image": { /* image data */ },
    "createdAt": "2025-01-16T12:00:00Z",
    "updatedAt": "2025-01-16T12:00:00Z"
  },
  {
    "id": 124,
    "topic": "AI in Business",
    "platform": "twitter",
    "tone": "casual",
    "imageCount": 2,
    "research": { /* research data */ },
    "content": { /* content data */ },
    "image": { /* image data */ },
    "createdAt": "2025-01-16T11:30:00Z",
    "updatedAt": "2025-01-16T11:30:00Z"
  }
]
```

**Integration Notes:**
- Content ordered by creation date (newest first)
- Use for content library/history views
- Extract `id` values for LinkedIn posting

### Get Specific Content

**Request:**
```http
GET /api/v1/content/123
Authorization: Bearer <access_token>
```

**Response:** *(Same format as generation response)*

**Error Response (404):**
```json
{
  "message": "Content not found or you don't have permission to access it",
  "status": 404,
  "timestamp": "2025-01-16T10:30:00Z"
}
```

### Delete Content

**Request:**
```http
DELETE /api/v1/content/123
Authorization: Bearer <access_token>
```

**Response:** `204 No Content` (Empty body)

---

## LinkedIn Integration

### Check LinkedIn Connection Status

Before posting, verify LinkedIn OAuth connection:

**Request:**
```http
GET /api/v1/social/linkedin/status
Authorization: Bearer <access_token>
```

**Response (Connected):**
```json
{
  "connected": true,
  "message": "LinkedIn connection is active and ready for posting"
}
```

**Response (Not Connected):**
```json
{
  "connected": false,
  "message": "LinkedIn connection required"
}
```

### Post Content to LinkedIn

**🔥 NEW DECOUPLED API - Uses Content ID Only**

**Request:**
```http
POST /api/v1/social/linkedin/post
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "id": 123
}
```

**Success Response:**
```json
{
  "postId": "urn:li:activity:123456789",
  "state": "PUBLISHED",
  "postUrl": "https://www.linkedin.com/posts/activity-123456789",
  "message": "Post created successfully via OAuth"
}
```

**Error Responses:**

**Content Not Found (400):**
```json
{
  "message": "Content not found or you don't have permission to access it",
  "status": 400,
  "timestamp": "2025-01-16T10:30:00Z"
}
```

**LinkedIn Not Connected (400):**
```json
{
  "message": "LinkedIn connection not found or expired. Please reconnect your LinkedIn account.",
  "status": 400,
  "timestamp": "2025-01-16T10:30:00Z"
}
```

**Integration Notes:**
- **Only content ID required** - Backend fetches all content details
- **User-scoped security** - Users can only post their own content
- **Automatic image handling** - Backend extracts and uploads images
- **OAuth validation** - Backend checks LinkedIn connection automatically

---

## Summary

The new integration provides:
- ✅ **Automatic content persistence** with unique IDs
- ✅ **Decoupled LinkedIn posting** using content IDs only
- ✅ **Complete content management** CRUD operations
- ✅ **Seamless workflows** from generation to social posting
- ✅ **Enhanced error handling** with actionable messages
- ✅ **User-scoped security** ensuring data privacy

**Key Integration Point:** The UI only needs to manage content IDs - the backend handles all content fetching, image processing, and LinkedIn posting logic.