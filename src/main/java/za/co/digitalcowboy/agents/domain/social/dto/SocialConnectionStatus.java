package za.co.digitalcowboy.agents.domain.social.dto;

public class SocialConnectionStatus {
    private final boolean connected;
    private final String message;
    
    public SocialConnectionStatus(boolean connected, String message) {
        this.connected = connected;
        this.message = message;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public String getMessage() {
        return message;
    }
}