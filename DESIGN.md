# Design Documentation: Java AI Agents System

## System Architecture

### Overview

The Java AI Agents system implements a sequential workflow orchestrating three specialized AI agents. Each agent has a specific responsibility and contributes to the final result through a stateful graph execution model.

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Research Agent │───▶│  Content Agent  │───▶│   Image Agent   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
   5-7 Factual            Platform-Specific       AI-Generated
   Bullet Points           Content + Tone            Images
```

### Core Components

#### 1. Agent State Management

**Class**: `AgentState`
- **Purpose**: Centralized state container for the workflow
- **Pattern**: State Machine with immutable updates
- **Lifecycle**: Created → Research → Content → Image → Result

```java
AgentState state = new AgentState(request);
// Each agent updates its portion of the state
state.setResearch(researchPoints);
state.setContent(contentDraft);  
state.setImage(imageResult);
```

#### 2. Agent Orchestration

**Class**: `AgentGraph`
- **Purpose**: Coordinates agent execution in sequence
- **Pattern**: Pipeline with error isolation
- **Benefits**: 
  - Clear execution order
  - Independent error handling per node
  - Comprehensive metrics collection

#### 3. Prompt Engineering

**Classes**: `ResearchPrompt`, `ContentPrompt`, `ImagePrompt`
- **Purpose**: Structured prompt templates with JSON schema enforcement
- **Pattern**: Template Method with validation
- **Features**:
  - Deterministic outputs via JSON schema
  - Platform-specific constraints
  - Tone adaptation logic

## Agent Design Patterns

### Research Agent

**Responsibility**: Convert topic to structured research points
**Input**: Topic string
**Output**: 5-7 factual bullet points with optional sources
**Constraints**: 
- Max 25 words per bullet
- Neutral, verifiable facts
- No marketing language

**Design Decisions**:
- **JSON Schema Validation**: Ensures consistent structure
- **Fallback Strategy**: Returns error message as research point
- **Word Count Validation**: Warns on constraint violations

```java
ResearchPoints research(String topic) {
    // 1. Generate structured prompt
    // 2. Call LLM with schema constraints  
    // 3. Parse and validate JSON response
    // 4. Apply business rules (5-7 points, 25 words max)
    // 5. Return structured result with fallback
}
```

### Content Agent

**Responsibility**: Transform research into platform-specific content
**Input**: Research points + platform + tone
**Output**: Formatted content with headline, body, CTA
**Constraints**: Platform-specific length and formatting rules

**Platform Adaptations**:

| Platform | Constraints | Format |
|----------|-------------|---------|
| Twitter | ≤280 chars | Single paragraph + hashtags |
| LinkedIn | 3-5 paragraphs | Professional structure + CTA |
| Instagram | Caption style | Line breaks + hashtags |
| Blog | 300-500 words | H2/H3 structure + sections |

**Design Decisions**:
- **Platform Strategy Pattern**: Different validation per platform
- **Tone Injection**: System prompts adapted by tone parameter
- **Structured Output**: Consistent JSON schema across platforms

### Image Agent

**Responsibility**: Generate relevant images from content
**Input**: Content draft + image count
**Output**: Image URLs from OpenAI Images API
**Process**: Content → Image Brief → API Call → URLs

**Two-Step Process**:
1. **Brief Generation**: LLM creates image description from content
2. **Image Generation**: OpenAI Images API creates actual images

**Design Decisions**:
- **Separation of Concerns**: Prompt generation separate from image creation
- **API Abstraction**: `OpenAiImageTool` encapsulates external API
- **Error Resilience**: Empty result list on failures

## Error Handling Strategy

### Graceful Degradation

The system continues execution even when individual agents fail:

```
Research Fails → Empty bullet points → Content uses topic directly
Content Fails → Generic content → Image uses topic for prompt
Image Fails → Empty URL list → User gets text-only result
```

### Error Boundaries

Each agent has isolated error handling:

```java
try {
    return timer.recordCallable(() -> {
        // Agent logic
    });
} catch (Exception e) {
    log.error("Agent failed", e);
    return fallbackResult();
}
```

**Benefits**:
- **Resilience**: Partial failures don't break the workflow
- **Observability**: Clear error logging per agent
- **User Experience**: Always returns a usable response

### Validation Strategy

**Input Validation**: Spring Boot validation annotations
**Output Validation**: Custom business rule enforcement
**Schema Validation**: JSON schema compliance checking

## Observability Design

### Metrics Collection

Each component exposes timing metrics:

```java
@Bean Timer researchAgentTimer(MeterRegistry registry)
@Bean Timer contentAgentTimer(MeterRegistry registry)  
@Bean Timer imageAgentTimer(MeterRegistry registry)
@Bean Timer orchestrationTimer(MeterRegistry registry)
```

**Metric Naming Convention**: `{component}.{operation}.duration`

### Logging Strategy

**Structured Logging**: JSON format with correlation IDs
**Log Levels**:
- `DEBUG`: Agent inputs/outputs, prompt details
- `INFO`: Workflow start/completion, high-level operations
- `WARN`: Constraint violations, performance issues
- `ERROR`: Agent failures, API errors

### Health Monitoring

**Spring Boot Actuator Endpoints**:
- `/actuator/health`: Basic service health
- `/actuator/metrics`: Performance metrics
- `/actuator/info`: Build and version info

## Testing Strategy

### Test Pyramid

**Unit Tests**: Individual agent behavior
**Integration Tests**: End-to-end workflows
**Contract Tests**: OpenAI API interaction validation

### Mock Strategy

**LLM Mocking**: Predefined responses for consistent testing
**API Mocking**: Stubbed OpenAI Images API responses
**Timer Mocking**: SimpleMeterRegistry for test isolation

### Constraint Testing

**Research Validation**: 5-7 bullet points, 25 words max
**Platform Validation**: Length limits, format requirements
**Error Scenarios**: API failures, malformed responses

## Security Considerations

### API Key Management

- **Environment Variables**: No hardcoded secrets
- **Configuration Validation**: Startup failure on missing keys
- **Logging Safety**: API keys never logged

### Input Validation

- **Bean Validation**: Annotation-based input validation
- **Enum Constraints**: Platform and tone validation
- **Length Limits**: Prevent prompt injection via long inputs

### Output Sanitization

- **JSON Parsing**: Strict schema validation
- **Content Filtering**: Platform-appropriate content validation
- **URL Validation**: Image URL format verification

## Performance Considerations

### Deterministic Settings

```yaml
openai:
  temperature: 0.1    # Low temperature for consistent output
  max-tokens: 2000    # Reasonable limit for response size
```

**Benefits**:
- **Predictable**: Similar inputs produce similar outputs
- **Testable**: Consistent behavior in tests
- **Cost-Effective**: Token limits control API costs

### Timeout Configuration

```yaml
openai:
  timeout-ms: 30000   # 30-second timeout for reliability
```

**Strategy**: Balance between user experience and API reliability

### Memory Management

- **Stateless Agents**: No persistent state between requests
- **Lightweight Objects**: Records instead of heavy classes
- **Resource Cleanup**: Try-with-resources for HTTP connections

## Scalability Architecture

### Stateless Design

All agents are stateless, enabling horizontal scaling:
- **No Session State**: Each request is independent
- **Shared Configuration**: Environment-based config
- **Database-Free**: No persistent storage requirements

### Cloud-Native Features

- **12-Factor Compliance**: Configuration via environment
- **Health Checks**: Load balancer integration ready
- **Metrics Export**: Prometheus/Grafana compatible
- **Container Ready**: Minimal Docker image

## Future Enhancement Patterns

### Async Processing

```java
@Async
CompletableFuture<OrchestrationResult> runAsync(TopicRequest request)
```

**Benefits**: Handle long-running requests without blocking

### Caching Strategy

```java
@Cacheable("research")
ResearchPoints research(String topic)
```

**Benefits**: Reduce API calls for similar topics

### Vector Memory Integration

```java
@Service
class PersonalizationService {
    VectorStore brandVoiceStore;
    VectorStore contentHistoryStore;
}
```

**Benefits**: Learn user preferences and brand voice over time

This design balances simplicity with extensibility, providing a solid foundation for production deployment while enabling future enhancements.