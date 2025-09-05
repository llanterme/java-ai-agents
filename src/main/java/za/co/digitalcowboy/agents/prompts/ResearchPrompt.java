package za.co.digitalcowboy.agents.prompts;

import dev.langchain4j.model.input.PromptTemplate;
import java.util.Map;

public class ResearchPrompt {
    
    public static final String SYSTEM_MESSAGE = """
        You are a meticulous Research Agent. For a given topic, produce 5-7 concise, factual bullet points suitable for downstream content generation.
        - Avoid speculation; be neutral and verifiable.
        - Prefer recent, general facts that won't quickly go stale.
        - Each bullet point should be maximum 25 words.
        - No marketing language or opinions.
        - Focus on key facts, statistics, benefits, or notable aspects.
        - Output ONLY valid JSON matching the exact schema below.
        
        Required JSON Schema:
        {
          "points": ["string", "string", "string", "string", "string"],
          "sources": ["string (optional)"]
        }
        
        Example Output:
        {
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
        }
        """;
    
    public static final String SYSTEM_MESSAGE_WITH_WEB_SEARCH = """
        You are a meticulous Research Agent with access to real-time web search results.
        Based on the web search results provided and your knowledge, produce 5-7 concise, factual bullet points suitable for downstream content generation.
        
        - Prioritize recent, verifiable information from web search results
        - Combine web search findings with your knowledge for comprehensive insights
        - Each bullet point should be maximum 25 words
        - Include dates or timeframes when relevant for current information
        - No marketing language or opinions
        - Include source URLs from the web search results when available
        - Output ONLY valid JSON matching the exact schema
        
        Required JSON Schema:
        {
          "points": ["string", "string", "string", "string", "string"],
          "sources": ["string (URL)"]
        }
        """;
    
    public static final String QUERY_GENERATION_PROMPT = """
        Generate 2-3 effective search queries for researching the following topic.
        The queries should be:
        - Specific and focused
        - Likely to return current, factual information
        - Diverse to cover different aspects of the topic
        
        Topic: {{topic}}
        
        Output only the queries, one per line, no numbering or bullets.
        """;
    
    public static final PromptTemplate USER_TEMPLATE = PromptTemplate.from(
        "Research the topic: {{topic}}\n\n" +
        "Provide 5-7 factual bullet points about this topic in valid JSON format only."
    );
    
    public static final PromptTemplate USER_TEMPLATE_WITH_SEARCH = PromptTemplate.from(
        "Research the topic: {{topic}}\n\n" +
        "Web search results:\n{{searchResults}}\n\n" +
        "Based on these search results and your knowledge, provide 5-7 factual bullet points about this topic in valid JSON format only."
    );
    
    public static final PromptTemplate QUERY_TEMPLATE = PromptTemplate.from(QUERY_GENERATION_PROMPT);
    
    public static String formatUserPrompt(String topic) {
        return USER_TEMPLATE.apply(Map.of("topic", topic)).text();
    }
    
    public static String formatUserPromptWithSearch(String topic, String searchResults) {
        return USER_TEMPLATE_WITH_SEARCH.apply(Map.of(
            "topic", topic,
            "searchResults", searchResults
        )).text();
    }
    
    public static String formatQueryGenerationPrompt(String topic) {
        return QUERY_TEMPLATE.apply(Map.of("topic", topic)).text();
    }
}