package za.co.digitalcowboy.agents.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.co.digitalcowboy.agents.domain.User;
import za.co.digitalcowboy.agents.domain.oauth.ConnectedAccount;
import za.co.digitalcowboy.agents.domain.oauth.dto.ConnectResponse;
import za.co.digitalcowboy.agents.domain.oauth.dto.ConnectionStatusResponse;
import za.co.digitalcowboy.agents.domain.oauth.dto.DisconnectResponse;
import za.co.digitalcowboy.agents.service.oauth.OAuthConnectionService;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionController.class);
    
    private final OAuthConnectionService oAuthConnectionService;
    
    @Autowired
    public ConnectionController(OAuthConnectionService oAuthConnectionService) {
        this.oAuthConnectionService = oAuthConnectionService;
    }
    
    @PostMapping("/{provider}/connect")
    public ResponseEntity<ConnectResponse> initiateConnection(
            @PathVariable String provider,
            @RequestParam(name = "redirect_uri", required = false) String redirectUri,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            
            // Use default redirect URI if not provided
            if (redirectUri == null || redirectUri.trim().isEmpty()) {
                redirectUri = "http://localhost:8080/api/v1/connections/" + provider + "/callback";
            }
            
            String authorizationUrl = oAuthConnectionService.initiateConnection(provider, user.getId(), redirectUri);
            
            ConnectResponse response = new ConnectResponse(authorizationUrl, provider);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid provider or request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.error("Connection already exists: {}", e.getMessage());
            return ResponseEntity.status(409).build();
        } catch (Exception e) {
            logger.error("Failed to initiate OAuth connection for provider {}", provider, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> handleCallback(
            @PathVariable String provider,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            @RequestParam("state") String state,
            @RequestParam(name = "redirect_uri", required = false) String redirectUri) {
        
        try {
            // Handle OAuth error responses first
            if (error != null) {
                logger.error("OAuth error from {}: {} - {}", provider, error, errorDescription);
                String errorUrl = "http://localhost:3000/settings/connections?status=error&provider=" + provider + 
                                "&error=" + error + "&error_description=" + (errorDescription != null ? errorDescription : "");
                return ResponseEntity.status(302)
                        .location(URI.create(errorUrl))
                        .build();
            }
            
            // Ensure we have a code parameter for successful OAuth flow
            if (code == null || code.trim().isEmpty()) {
                logger.error("OAuth callback missing code parameter for provider: {}", provider);
                String errorUrl = "http://localhost:3000/settings/connections?status=error&provider=" + provider + "&error=missing_code";
                return ResponseEntity.status(302)
                        .location(URI.create(errorUrl))
                        .build();
            }
            
            // Use default redirect URI if not provided
            if (redirectUri == null || redirectUri.trim().isEmpty()) {
                redirectUri = "http://localhost:8080/api/v1/connections/" + provider + "/callback";
            }
            
            String frontendRedirectUrl = oAuthConnectionService.handleCallback(provider, code, state, redirectUri);
            
            return ResponseEntity.status(302)
                    .location(URI.create(frontendRedirectUrl))
                    .build();
                    
        } catch (IllegalArgumentException e) {
            logger.error("Invalid callback parameters: {}", e.getMessage());
            String errorUrl = "http://localhost:3000/settings/connections?status=error&provider=" + provider + "&error=invalid_request";
            return ResponseEntity.status(302)
                    .location(URI.create(errorUrl))
                    .build();
        } catch (Exception e) {
            logger.error("Failed to handle OAuth callback for provider {}", provider, e);
            String errorUrl = "http://localhost:3000/settings/connections?status=error&provider=" + provider + "&error=callback_failed";
            return ResponseEntity.status(302)
                    .location(URI.create(errorUrl))
                    .build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<ConnectionStatusResponse>> listConnections(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<ConnectedAccount> connections = oAuthConnectionService.getUserConnections(user.getId());
            
            List<ConnectionStatusResponse> response = connections.stream()
                    .map(this::toConnectionStatusResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to list user connections", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{provider}/status")
    public ResponseEntity<ConnectionStatusResponse> getConnectionStatus(
            @PathVariable String provider,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            boolean isConnected = oAuthConnectionService.isConnected(user.getId(), provider);
            
            if (!isConnected) {
                ConnectionStatusResponse response = new ConnectionStatusResponse(provider, false, null);
                return ResponseEntity.ok(response);
            }
            
            List<ConnectedAccount> connections = oAuthConnectionService.getUserConnections(user.getId());
            ConnectedAccount connection = connections.stream()
                    .filter(c -> c.getProvider().getValue().equals(provider))
                    .findFirst()
                    .orElse(null);
            
            if (connection == null) {
                ConnectionStatusResponse response = new ConnectionStatusResponse(provider, false, null);
                return ResponseEntity.ok(response);
            }
            
            ConnectionStatusResponse response = toConnectionStatusResponse(connection);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid provider: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to get connection status for provider {}", provider, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{provider}")
    public ResponseEntity<DisconnectResponse> disconnectAccount(
            @PathVariable String provider,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            oAuthConnectionService.disconnectAccount(user.getId(), provider);
            
            DisconnectResponse response = DisconnectResponse.success(provider);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid provider or connection not found: {}", e.getMessage());
            DisconnectResponse response = DisconnectResponse.error(provider, "Connection not found");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to disconnect from provider {}", provider, e);
            DisconnectResponse response = DisconnectResponse.error(provider, "Failed to disconnect");
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{provider}/refresh")
    public ResponseEntity<ConnectionStatusResponse> refreshConnection(
            @PathVariable String provider,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            boolean refreshed = oAuthConnectionService.refreshTokenIfNeeded(user.getId(), provider);
            
            if (refreshed) {
                // Return updated connection status
                return getConnectionStatus(provider, authentication);
            } else {
                DisconnectResponse errorResponse = DisconnectResponse.error(provider, "Failed to refresh token");
                return ResponseEntity.badRequest().build();
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid provider: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to refresh token for provider {}", provider, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private ConnectionStatusResponse toConnectionStatusResponse(ConnectedAccount account) {
        ConnectionStatusResponse response = new ConnectionStatusResponse(
                account.getProvider().getValue(),
                account.isActive(),
                account.getStatus()
        );
        
        response.setProviderUsername(account.getProviderUsername());
        response.setScopes(account.getScopesList());
        response.setConnectedAt(account.getConnectedAt());
        response.setExpiresAt(account.getTokenExpiresAt());
        response.setExpired(account.isExpired());
        
        return response;
    }
}