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

Generates content using the agent workflow.

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
curl -X POST http://localhost:8080/api/v1/generate \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Machine Learning",
    "platform": "twitter", 
    "tone": "casual",
    "imageCount": 1
  }'
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

### Local Image URLs (Latest)
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

**Last Updated**: 2025-09-04
**Version**: 1.0.0
**LangChain4j**: 0.27.1
**Spring Boot**: 3.2.0

For questions or contributions, refer to the comprehensive test suite and existing patterns in the codebase.