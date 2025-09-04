# Java AI Agents System

A production-ready Java service that orchestrates AI agents using OpenAI APIs. The system coordinates three specialized agents to research topics, create platform-specific content, and generate relevant images.

## Architecture

The system uses a sequential workflow with three specialized agents:

1. **Research Agent** → Produces 5-7 factual bullet points about a topic
2. **Content Agent** → Creates platform-specific content with the requested tone
3. **Image Agent** → Generates relevant images via OpenAI Images API

## Tech Stack

- **Java 17+** with **Spring Boot 3+**
- **LangChain4j** for LLM integration
- **OpenAI APIs** for text generation (GPT-4o) and image generation (DALL·E-3)
- **Maven** for build management
- **Micrometer** for metrics and observability

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- OpenAI API key

### Environment Setup

```bash
export OPENAI_API_KEY=your_openai_api_key_here
export OPENAI_TEXT_MODEL=gpt-4o          # Optional, defaults to gpt-4o
export OPENAI_IMAGE_MODEL=dall-e-3       # Optional, defaults to dall-e-3
export OPENAI_TIMEOUT_MS=30000          # Optional, defaults to 30000
```

### Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/java-ai-agents-1.0.0.jar
```

The application starts on port 8080 by default.

### Health Check

```bash
curl http://localhost:8080/api/v1/health
```

## API Reference

### Generate Content

**POST** `/api/v1/generate`

Creates research, content, and images for a given topic.

#### Request Body

```json
{
  "topic": "Artificial Intelligence",
  "platform": "twitter",
  "tone": "professional", 
  "imageCount": 1
}
```

#### Parameters

- `topic` (string, required): The topic to research and create content about
- `platform` (string, required): Target platform - one of `twitter`, `linkedin`, `instagram`, `blog`
- `tone` (string, required): Content tone - one of `professional`, `casual`, `playful`, `authoritative`
- `imageCount` (integer, optional): Number of images to generate (default: 1)

#### Response

```json
{
  "topic": "Artificial Intelligence",
  "research": {
    "points": [
      "Artificial Intelligence refers to computer systems that can perform tasks typically requiring human intelligence.",
      "Machine Learning is a subset of AI that enables computers to learn without explicit programming.",
      "Deep Learning uses neural networks with multiple layers to process complex data patterns.",
      "AI applications include image recognition, natural language processing, and autonomous vehicles.",
      "The global AI market was valued at approximately $136 billion in 2022.",
      "Major AI companies include Google, Microsoft, OpenAI, and NVIDIA.",
      "AI ethics and responsible development are increasingly important considerations."
    ],
    "sources": []
  },
  "content": {
    "platform": "twitter",
    "tone": "professional",
    "headline": "AI Revolution",
    "body": "Artificial Intelligence is transforming industries with $136B market value. From machine learning to deep learning, AI applications span image recognition to autonomous vehicles. Major players: Google, Microsoft, OpenAI, NVIDIA. Ethics matter! #AI #TechTrends",
    "cta": "Learn more about AI trends"
  },
  "image": {
    "prompt": "Modern AI technology visualization with neural networks, futuristic and professional style, clean background",
    "openAiImageUrls": [
      "https://oaidalleapiprodscus.blob.core.windows.net/private/org-xyz/image1.png"
    ]
  }
}
```

#### Platform-Specific Constraints

- **Twitter**: ≤ 280 characters total, 1-2 hashtags, concise format
- **LinkedIn**: 3-5 paragraphs, professional tone, meaningful insights
- **Instagram**: Caption style with line breaks, 2-3 hashtags, visual language
- **Blog**: 300-500 words, structured with headings, comprehensive content

#### Example Requests

**Twitter Professional**
```bash
curl -X POST http://localhost:8080/api/v1/generate \\
  -H "Content-Type: application/json" \\
  -d '{
    "topic": "Renewable Energy",
    "platform": "twitter",
    "tone": "professional",
    "imageCount": 1
  }'
```

**LinkedIn Casual**
```bash
curl -X POST http://localhost:8080/api/v1/generate \\
  -H "Content-Type: application/json" \\
  -d '{
    "topic": "Remote Work Trends",
    "platform": "linkedin", 
    "tone": "casual",
    "imageCount": 2
  }'
```

**Blog Authoritative**
```bash
curl -X POST http://localhost:8080/api/v1/generate \\
  -H "Content-Type: application/json" \\
  -d '{
    "topic": "Cybersecurity Best Practices",
    "platform": "blog",
    "tone": "authoritative",
    "imageCount": 1
  }'
```

## Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
openai:
  api-key: ${OPENAI_API_KEY:}
  text-model: ${OPENAI_TEXT_MODEL:gpt-4o}
  image-model: ${OPENAI_IMAGE_MODEL:dall-e-3}
  timeout-ms: ${OPENAI_TIMEOUT_MS:30000}
  temperature: 0.1
  max-tokens: 2000

server:
  port: 8080

logging:
  level:
    com.example.agentic: DEBUG
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key (required) | - |
| `OPENAI_TEXT_MODEL` | OpenAI text model | `gpt-4o` |
| `OPENAI_IMAGE_MODEL` | OpenAI image model | `dall-e-3` |
| `OPENAI_TIMEOUT_MS` | Request timeout in milliseconds | `30000` |

## Monitoring

### Health Endpoints

- **Health**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Info**: `GET /actuator/info`

### Metrics

The application exposes custom metrics via Micrometer:

- `agent.research.duration` - Time taken by research agent
- `agent.content.duration` - Time taken by content agent  
- `agent.image.duration` - Time taken by image agent
- `orchestration.duration` - Total orchestration time

### Logging

Structured logging with configurable levels:

```yaml
logging:
  level:
    com.example.agentic: DEBUG    # Application logs
    dev.langchain4j: INFO         # LangChain4j logs
    org.springframework: WARN     # Spring logs
```

## Testing

### Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AgentFlowTests

# Run with coverage
mvn test jacoco:report
```

### Test Coverage

The test suite includes:

- **Unit tests** for individual agents
- **Integration tests** for the complete workflow
- **Platform-specific validation** tests
- **Error handling** scenarios
- **Constraint validation** tests

## Error Handling

The system includes comprehensive error handling:

- **Graceful degradation**: Failed agents don't stop the workflow
- **Fallback responses**: Meaningful error messages for users
- **Retry logic**: Built-in retry for transient failures
- **Validation**: Input validation with detailed error messages

### Error Response Format

```json
{
  "message": "Validation failed",
  "status": 400,
  "timestamp": "2024-01-15T10:30:00",
  "details": {
    "topic": "Topic is required",
    "platform": "Platform must be one of: twitter, linkedin, instagram, blog"
  }
}
```

## Development

### Project Structure

```
src/
├── main/java/com/example/agentic/
│   ├── api/                 # REST controllers
│   ├── agents/              # Agent implementations
│   ├── config/              # Configuration classes
│   ├── domain/              # Domain models
│   ├── graph/               # Workflow orchestration
│   ├── prompts/             # Prompt templates
│   └── tools/               # External API integrations
└── test/java/com/example/agentic/
    └── AgentFlowTests.java  # Test suite
```

### Adding New Platforms

1. Update `TopicRequest` validation pattern
2. Add platform-specific logic in `ContentAgent`
3. Update prompt templates
4. Add validation tests

### Adding New Tones

1. Update `TopicRequest` validation pattern  
2. Update prompt templates with tone guidelines
3. Add tone-specific validation tests

## Deployment

### Docker

```dockerfile
FROM openjdk:17-jdk-alpine
COPY target/java-ai-agents-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Cloud Deployment

The application is cloud-ready with:

- **12-factor app** compliance
- **Environment-based** configuration
- **Health checks** for load balancers
- **Metrics** for monitoring
- **Structured logging** for observability

## Design Decisions

### Why Sequential Workflow?

- **Simplicity**: Easy to understand and debug
- **Reliability**: Each step builds on the previous
- **Error isolation**: Failed steps don't affect others
- **Observability**: Clear metrics per step

### Why Fallback Responses?

- **Resilience**: System remains functional even with API failures
- **User experience**: Always returns meaningful responses
- **Debugging**: Clear error messages for troubleshooting

### Why Platform-Specific Validation?

- **Quality**: Ensures content meets platform requirements
- **User satisfaction**: Content fits the intended platform
- **Compliance**: Adheres to platform character limits and formats

## Future Enhancements

- **Source gathering**: Add web search for research citations
- **Vector memory**: Personalize tone and brand voice
- **Async processing**: Handle long-running requests
- **Caching**: Cache research for similar topics
- **A/B testing**: Compare different content variations

## License

This project is licensed under the MIT License - see the LICENSE file for details.