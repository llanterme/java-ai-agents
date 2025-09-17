package za.co.digitalcowboy.agents.domain.oauth;

public enum OAuthProvider {
    LINKEDIN("linkedin"),
    FACEBOOK("facebook"),
    TWITTER("twitter"),
    INSTAGRAM("instagram");
    
    private final String value;
    
    OAuthProvider(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static OAuthProvider fromValue(String value) {
        for (OAuthProvider provider : values()) {
            if (provider.value.equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown OAuth provider: " + value);
    }
}