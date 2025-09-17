package za.co.digitalcowboy.agents.service.oauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.digitalcowboy.agents.domain.oauth.OAuthProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OAuthProviderFactory {
    
    private final Map<OAuthProvider, OAuthProviderService> providers;
    
    @Autowired
    public OAuthProviderFactory(List<OAuthProviderService> providerServices) {
        this.providers = new HashMap<>();
        for (OAuthProviderService service : providerServices) {
            OAuthProvider provider = OAuthProvider.fromValue(service.getProviderName());
            providers.put(provider, service);
        }
    }
    
    public OAuthProviderService getProvider(OAuthProvider provider) {
        OAuthProviderService service = providers.get(provider);
        if (service == null) {
            throw new IllegalArgumentException("No OAuth provider service found for: " + provider);
        }
        return service;
    }
    
    public OAuthProviderService getProvider(String providerName) {
        return getProvider(OAuthProvider.fromValue(providerName));
    }
    
    public boolean isProviderSupported(OAuthProvider provider) {
        return providers.containsKey(provider);
    }
    
    public boolean isProviderSupported(String providerName) {
        try {
            return isProviderSupported(OAuthProvider.fromValue(providerName));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}