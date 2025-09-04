package za.co.digitalcowboy.agents.prompts;

import dev.langchain4j.model.input.PromptTemplate;
import za.co.digitalcowboy.agents.domain.ResearchPoints;
import java.util.Map;

public class ContentPrompt {
    
    public static final String SYSTEM_MESSAGE = """
        You are a Content Agent. Transform the research into platform-specific content with the requested tone.
        
        Platform Constraints:
        - twitter: ≤ 280 characters total; 1-2 relevant hashtags; strong hook; concise and engaging
        - linkedin: 3-5 short paragraphs; professional tone regardless of requested tone; meaningful insights; 1 CTA
        - instagram: caption-style with line breaks; 2-3 friendly hashtags; engaging and visual language; 1 CTA  
        - blog: 300-500 words; clear structure with sections; intro, body, conclusion; 1 CTA; informative and comprehensive
        
        Tone Guidelines:
        - professional: formal, authoritative, business-focused
        - casual: conversational, friendly, approachable
        - playful: fun, energetic, creative, light-hearted
        - authoritative: expert, confident, educational, fact-driven
        
        Always reflect the requested tone exactly while respecting platform constraints.
        
        Output ONLY valid JSON matching this schema:
        {
          "platform": "string",
          "tone": "string", 
          "headline": "string",
          "body": "string",
          "cta": "string"
        }
        """;
    
    public static final PromptTemplate USER_TEMPLATE = PromptTemplate.from(
        "Transform this research into {{platform}} content with {{tone}} tone:\n\n" +
        "Research Points:\n{{research}}\n\n" +
        "Create platform-appropriate content in valid JSON format only."
    );
    
    public static String formatUserPrompt(ResearchPoints research, String platform, String tone) {
        String researchText = String.join("\n", research.points().stream()
            .map(point -> "• " + point)
            .toList());
            
        return USER_TEMPLATE.apply(Map.of(
            "research", researchText,
            "platform", platform,
            "tone", tone
        )).text();
    }
}