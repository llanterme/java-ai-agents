#!/bin/bash

# Script to decrypt LinkedIn access token from database and test LinkedIn API
# Usage: ./decrypt_and_test_linkedin.sh [user_id]

set -e

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-java_ai_agents}"
DB_USERNAME="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:-Passw0rd1}"
USER_ID="${1:-1}"  # Default to user ID 1 if not provided

echo "🔍 Fetching encrypted token from database..."

# Query database to get encrypted access token for LinkedIn
ENCRYPTED_TOKEN=$(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -s -N -e "
SELECT access_token 
FROM connected_accounts 
WHERE user_id = $USER_ID AND provider = 'linkedin' AND status = 'ACTIVE'
LIMIT 1;
")

if [ -z "$ENCRYPTED_TOKEN" ]; then
    echo "❌ No LinkedIn token found for user ID $USER_ID"
    echo "   Make sure the user has connected their LinkedIn account"
    exit 1
fi

echo "✅ Found encrypted token in database"
echo "📄 Encrypted token (first 50 chars): ${ENCRYPTED_TOKEN:0:50}..."

# Create temporary Java class to decrypt the token
TEMP_DIR=$(mktemp -d)
JAVA_FILE="$TEMP_DIR/TokenDecryptor.java"

cat > "$JAVA_FILE" << 'EOF'
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenDecryptor {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java TokenDecryptor <encryptionKey> <encryptedToken>");
            System.exit(1);
        }
        
        String encryptionKeyBase64 = args[0];
        String encryptedText = args[1];
        
        try {
            // Decode the encryption key
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKeyBase64);
            SecretKeySpec secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
            
            // Decode the encrypted data
            byte[] decodedData = Base64.getDecoder().decode(encryptedText);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[decodedData.length - GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decodedData, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            // Decrypt
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            
            byte[] decryptedData = cipher.doFinal(encryptedData);
            String decryptedToken = new String(decryptedData, StandardCharsets.UTF_8);
            
            System.out.println(decryptedToken);
            
        } catch (Exception e) {
            System.err.println("Decryption failed: " + e.getMessage());
            System.exit(1);
        }
    }
}
EOF

echo "🔐 Decrypting token..."

# Check if OAUTH_ENCRYPTION_KEY is set
if [ -z "$OAUTH_ENCRYPTION_KEY" ]; then
    echo "❌ OAUTH_ENCRYPTION_KEY environment variable is not set"
    echo "   This should be the same key used by the Spring Boot application"
    echo "   Example: export OAUTH_ENCRYPTION_KEY=\$(openssl rand -base64 32)"
    rm -rf "$TEMP_DIR"
    exit 1
fi

# Compile and run the Java decryptor
cd "$TEMP_DIR"
javac TokenDecryptor.java
DECRYPTED_TOKEN=$(java TokenDecryptor "$OAUTH_ENCRYPTION_KEY" "$ENCRYPTED_TOKEN")

if [ $? -ne 0 ]; then
    echo "❌ Failed to decrypt token"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo "✅ Token decrypted successfully"
echo "🎫 Decrypted token (first 20 chars): ${DECRYPTED_TOKEN:0:20}..."

# Clean up temporary files
rm -rf "$TEMP_DIR"

echo ""
echo "🚀 Testing LinkedIn API with decrypted token..."
echo ""

# Test LinkedIn API with the decrypted token
HTTP_STATUS=$(curl -w "%{http_code}" -s -o /tmp/linkedin_response.json \
    -X GET "https://api.linkedin.com/v2/userinfo" \
    -H "Authorization: Bearer $DECRYPTED_TOKEN" \
    -H "X-Restli-Protocol-Version: 2.0.0")

echo "📡 HTTP Status: $HTTP_STATUS"

if [ "$HTTP_STATUS" = "200" ]; then
    echo "✅ LinkedIn API call successful!"
    echo ""
    echo "📋 User Info Response:"
    cat /tmp/linkedin_response.json | python3 -m json.tool 2>/dev/null || cat /tmp/linkedin_response.json
else
    echo "❌ LinkedIn API call failed"
    echo ""
    echo "📋 Error Response:"
    cat /tmp/linkedin_response.json
fi

echo ""
echo "🧪 Full curl command for manual testing:"
echo "curl -X GET \"https://api.linkedin.com/v2/userinfo\" \\"
echo "     -H \"Authorization: Bearer $DECRYPTED_TOKEN\" \\"
echo "     -H \"X-Restli-Protocol-Version: 2.0.0\""

# Clean up response file
rm -f /tmp/linkedin_response.json

echo ""
echo "✅ Script completed"