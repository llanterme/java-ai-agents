# Design Documentation: Java AI Agents System

## System Architecture

### Overview

The Java AI Agents system implements a sequential workflow orchestrating three specialized AI agents. Each agent has a specific responsibility and contributes to the final result through a stateful graph execution model.

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Research Agent │───▶│  Content Agent  │───▶│   Image Agent   │
│  + Web Search   │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
   5-7 Factual            Platform-Specific       AI-Generated
   Bullet Points           Content + Tone            Images
   + Source URLs                                   (2min timeout)
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

**Responsibility**: Convert topic to structured research points with real-time web data
**Input**: Topic string
**Output**: 5-7 factual bullet points with source URLs
**Constraints**: 
- Max 25 words per bullet
- Neutral, verifiable facts
- No marketing language
- Real-time information when SERP API enabled

**Hybrid Research Architecture**:

```java
ResearchPoints research(String topic) {
    if (webSearchEnabled) {
        return researchWithWebSearch(topic);
    } else {
        return researchWithoutWebSearch(topic);
    }
}

private ResearchPoints researchWithWebSearch(String topic) {
    // 1. Generate 2-3 optimized search queries using LLM
    // 2. Search web using SERP API for each query
    // 3. Extract and combine search results
    // 4. Synthesize web data + LLM knowledge into bullet points
    // 5. Include source URLs for verification
    // 6. Cache results for 1 hour
}
```

**Design Decisions**:
- **Hybrid Approach**: Combines web search with LLM synthesis
- **Smart Query Generation**: LLM creates optimized search queries
- **Source Attribution**: Real URLs for fact verification
- **Graceful Fallback**: LLM-only research if search fails
- **Result Caching**: 1-hour cache via Caffeine to reduce API costs
- **JSON Schema Validation**: Ensures consistent structure across modes

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

**Responsibility**: Generate relevant images from content with extended timeouts
**Input**: Content draft + image count
**Output**: Image URLs, local paths, and HTTP URLs
**Process**: Content → Image Brief → API Call → Download → Serve

**Multi-Step Process**:
1. **Brief Generation**: LLM creates image description from content
2. **Image Generation**: OpenAI DALL-E API creates actual images (2min timeout)
3. **Local Download**: Images downloaded and stored locally
4. **HTTP Serving**: Static URLs provided for direct access

**Design Decisions**:
- **Dedicated Timeout**: Separate 2-minute timeout for DALL-E operations
- **Multi-Format Output**: OpenAI URLs, local paths, and HTTP endpoints
- **Local Storage**: Configurable download and caching system
- **Error Resilience**: Graceful degradation when image generation fails
- **API Abstraction**: `OpenAiImageTool` with dedicated HTTP client

## Error Handling Strategy

### Graceful Degradation

The system continues execution even when individual agents fail:

```
Web Search Fails → LLM-only research → Content gets cached knowledge
Research Fails → Empty bullet points → Content uses topic directly
Content Fails → Generic content → Image uses topic for prompt
Image Fails (timeout) → Empty URL list → User gets text-only result
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
  timeout-ms: 30000          # 30-second timeout for text generation
  image-timeout-ms: 120000   # 2-minute timeout for image generation
```

**Multi-Tier Timeout Strategy**:
- **Text Operations**: 30-second timeout for fast response
- **Image Operations**: 2-minute timeout for DALL-E generation
- **Web Search**: 30-second timeout for SERP API calls
- **User Experience**: Balance between responsiveness and reliability

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

## Web Search Integration Architecture

### SERP API Integration

**Purpose**: Enable real-time information retrieval for research accuracy
**API**: Google Search via SERP API
**Caching**: Caffeine-based 1-hour result caching

```java
@Service
public class SerpApiSearchService {
    @Cacheable(value = "webSearchCache", key = "#query")
    public WebSearchResponse search(String query);
    
    public List<WebSearchResponse> searchMultiple(List<String> queries);
}
```

### Domain Model

**SearchResult**: Individual search result with title, snippet, link
**WebSearchResponse**: Container for multiple results with metadata

```java
public record SearchResult(String title, String snippet, String link, String displayLink, String date, Integer position);
public record WebSearchResponse(String query, List<SearchResult> results, Map<String, Object> knowledgeGraph, Long totalResults, Double searchTime);
```

### Configuration Management

```yaml
serpapi:
  api-key: ${SERPAPI_KEY:}
  search-engine: google
  location: "United States"
  max-results: 5
  enabled: true
```

**Benefits**:
- **Current Information**: Access to latest data beyond LLM training cutoff
- **Source Attribution**: Verifiable facts with original URLs
- **Cost Control**: Configurable result limits and caching
- **Fallback Support**: Graceful degradation to LLM-only research

## Future Enhancement Patterns

### Async Processing

```java
@Async
CompletableFuture<OrchestrationResult> runAsync(TopicRequest request)
```

**Benefits**: Handle long-running requests without blocking

### Caching Strategy

**Web Search Caching** (Implemented):
```java
@Cacheable(value = "webSearchCache", key = "#query")
public WebSearchResponse search(String query)
```

**Future Research Caching**:
```java
@Cacheable("research")
ResearchPoints research(String topic)
```

**Benefits**: 
- **Cost Reduction**: Minimize SERP API calls for repeated searches
- **Performance**: Faster response for cached web searches
- **Reliability**: Cached results available if APIs fail

### Vector Memory Integration

```java
@Service
class PersonalizationService {
    VectorStore brandVoiceStore;
    VectorStore contentHistoryStore;
}
```

**Benefits**: Learn user preferences and brand voice over time

This design balances simplicity with extensibility, providing a solid foundation for production deployment. The addition of real-time web search capabilities and optimized timeout configurations demonstrates the system's evolution toward more intelligent and reliable AI-powered content generation.