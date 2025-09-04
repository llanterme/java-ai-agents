package za.co.digitalcowboy.agents.prompts;

import dev.langchain4j.model.input.PromptTemplate;
import za.co.digitalcowboy.agents.domain.ContentDraft;
import java.util.Map;

public class ImagePrompt {
    
    public static final String SYSTEM_MESSAGE = """
        You are an Image Agent that crafts precise image prompts from content drafts.
        
        Requirements:
        - Create a 1-2 sentence visual description based on the content
        - Include style hints (e.g., editorial, vector, photo-realistic, illustration, modern, minimalist)
        - Include composition details (subject, background, lighting, mood)  
        - Avoid text-in-image unless explicitly required
        - Make it relevant to the content topic and appropriate for the platform
        - Keep it concise but descriptive enough for high-quality image generation
        
        Output ONLY valid JSON matching this schema:
        {
          "prompt": "string"
        }
        
        Example Output:
        {
          "prompt": "Modern office workspace with AI technology elements, clean minimalist style, soft natural lighting, professional and innovative mood, no text"
        }
        """;
    
    public static final PromptTemplate USER_TEMPLATE = PromptTemplate.from(
        "Create an image prompt based on this content:\n\n" +
        "Platform: {{platform}}\n" +
        "Headline: {{headline}}\n" +
        "Content: {{body}}\n\n" +
        "Generate a precise image prompt in valid JSON format only."
    );
    
    public static String formatUserPrompt(ContentDraft content) {
        return USER_TEMPLATE.apply(Map.of(
            "platform", content.platform(),
            "headline", content.headline(),
            "body", content.body()
        )).text();
    }
}