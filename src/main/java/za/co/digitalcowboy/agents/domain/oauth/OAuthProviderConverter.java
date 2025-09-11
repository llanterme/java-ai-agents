package za.co.digitalcowboy.agents.domain.oauth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OAuthProviderConverter implements AttributeConverter<OAuthProvider, String> {
    
    @Override
    public String convertToDatabaseColumn(OAuthProvider provider) {
        return provider != null ? provider.getValue() : null;
    }
    
    @Override
    public OAuthProvider convertToEntityAttribute(String value) {
        return value != null ? OAuthProvider.fromValue(value) : null;
    }
}