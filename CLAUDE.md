# Java AI Agents System

A comprehensive Java-based agentic system that orchestrates multiple AI agents for content generation, research, and image creation using OpenAI APIs.

## Architecture Overview

This system implements a sequential agent workflow using **LangChain4j** and **Spring Boot 3+**:

```
User Request → Research Agent → Content Agent → Image Agent → Response
```

### Core Components

1. **Research Agent** (`ResearchAgent.java`): Generates 5-7 bullet points of research insights
2. **Content Agent** (`ContentAgent.java`): Creates platform-specific content (Twitter, LinkedIn, Instagram, Blog)
3. **Image Agent** (`ImageAgent.java`): Generates image briefs and creates images using DALL-E
4. **Agent Graph** (`AgentGraph.java`): Orchestrates the sequential workflow with error handling

## Technology Stack

- **Java 17+**
- **Spring Boot 3.2.0** - Web framework and dependency injection
- **LangChain4j 0.27.1** - LLM integration and prompt management
- **OpenAI APIs** - GPT-4o for text, DALL-E-3 for images
- **OkHttp** - HTTP client for external APIs
- **Maven** - Build and dependency management
- **JUnit 5** - Testing framework with comprehensive mocks

## Project Structure

```
src/main/java/za/co/digitalcowboy/agents/
├── agents/              # Core AI agents
├── config/              # Spring configuration
├── controller/          # REST API endpoints  
├── domain/              # Domain models and DTOs
├── graph/               # Agent orchestration
├── service/             # Async generation service
└── tools/               # External service integrations

src/test/java/
└── za/co/digitalcowboy/agents/
    └── AgentFlowTests.java  # Comprehensive test suite
```

## Key Features

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

### Error Handling & Resilience
- **Graceful Degradation**: Failed agents don't break the workflow
- **Fallback Responses**: Default content when APIs fail
- **Comprehensive Logging**: Structured logging with SLF4J
- **Metrics & Observability**: Micrometer timers for performance tracking

## Configuration

### Required Environment Variables

```bash
# OpenAI Configuration
OPENAI_API_KEY=your_openai_api_key_here

# Optional Configuration
OPENAI_TEXT_MODEL=gpt-4o                    # Default: gpt-4o
OPENAI_IMAGE_MODEL=dall-e-3                 # Default: dall-e-3
OPENAI_TIMEOUT_MS=30000                     # Default: 30000

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
  temperature: 0.1
  max-tokens: 2000

images:
  download-enabled: ${IMAGES_DOWNLOAD_ENABLED:true}
  local-storage-path: ${IMAGES_STORAGE_PATH:./generated-images}
  keep-remote-url: ${IMAGES_KEEP_REMOTE_URL:false}
  base-url: ${IMAGES_BASE_URL:http://localhost:8080}
```

## API Documentation

### POST /api/v1/generate

Generates content using the agent workflow (synchronous).

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

Starts asynchronous content generation and returns immediately with a task ID.

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

Returns the current status of an async generation task.

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

### GET /api/v1/generate/result/{taskId}

Returns the result of a completed async generation task.

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
- OpenAI API key

### Quick Start

1. **Clone and setup:**
```bash
git clone <repository>
cd java-ai-agents
```

2. **Set environment variables:**
```bash
export OPENAI_API_KEY=your_key_here
```

3. **Build and run:**
```bash
mvn clean compile
mvn spring-boot:run
```

4. **Test the API:**
```bash
# Synchronous generation (20+ seconds)
curl -X POST http://localhost:8080/api/v1/generate \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Machine Learning",
    "platform": "twitter", 
    "tone": "casual",
    "imageCount": 1
  }'

# Async generation (returns immediately)
curl -X POST http://localhost:8080/api/v1/generate/async \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Machine Learning",
    "platform": "twitter", 
    "tone": "casual",
    "imageCount": 1
  }'

# Check async task status
curl http://localhost:8080/api/v1/generate/status/TASK_ID

# Get async result (when completed)
curl http://localhost:8080/api/v1/generate/result/TASK_ID
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

### Async Generation System (Latest)
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

4. **Async Endpoint Taking 20+ Seconds (FIXED)**
```
Issue: /api/v1/generate/async was running synchronously instead of async
Cause: Spring @Async self-invocation problem in AsyncGenerationService
Fix: Replaced @Async with CompletableFuture.runAsync() to avoid proxy bypass
Result: Async endpoint now returns in ~50ms as expected
```

5. **Task Not Found After Restart**
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

**Last Updated**: 2025-09-04 (Added Async Generation System)
**Version**: 1.0.0
**LangChain4j**: 0.27.1
**Spring Boot**: 3.2.0

For questions or contributions, refer to the comprehensive test suite and existing patterns in the codebase.