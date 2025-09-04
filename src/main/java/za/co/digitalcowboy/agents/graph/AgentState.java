package za.co.digitalcowboy.agents.graph;

import za.co.digitalcowboy.agents.domain.*;

public class AgentState {
    private String topic;
    private String platform;
    private String tone;
    private int imageCount;
    
    private ResearchPoints research;
    private ContentDraft content;
    private ImageResult image;
    
    public AgentState() {
        this.research = ResearchPoints.empty();
        this.content = ContentDraft.empty();
        this.image = ImageResult.empty();
    }
    
    public AgentState(TopicRequest request) {
        this.topic = request.topic();
        this.platform = request.platform();
        this.tone = request.tone();
        this.imageCount = request.imageCount();
        this.research = ResearchPoints.empty();
        this.content = ContentDraft.empty();
        this.image = ImageResult.empty();
    }
    
    // Getters and setters
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    
    public int getImageCount() { return imageCount; }
    public void setImageCount(int imageCount) { this.imageCount = imageCount; }
    
    public ResearchPoints getResearch() { return research; }
    public void setResearch(ResearchPoints research) { this.research = research; }
    
    public ContentDraft getContent() { return content; }
    public void setContent(ContentDraft content) { this.content = content; }
    
    public ImageResult getImage() { return image; }
    public void setImage(ImageResult image) { this.image = image; }
    
    public OrchestrationResult toResult() {
        return new OrchestrationResult(topic, research, content, image);
    }
}