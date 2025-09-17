package za.co.digitalcowboy.agents.domain.oauth;

public enum ConnectionStatus {
    ACTIVE("active"),
    EXPIRED("expired"),
    REVOKED("revoked");
    
    private final String value;
    
    ConnectionStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static ConnectionStatus fromValue(String value) {
        for (ConnectionStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown connection status: " + value);
    }
}