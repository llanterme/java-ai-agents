package za.co.digitalcowboy.agents.tools;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ImageDownloadService {
    
    private static final Logger log = LoggerFactory.getLogger(ImageDownloadService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final OkHttpClient httpClient;
    private final boolean downloadEnabled;
    private final String storagePath;
    
    public ImageDownloadService(
            OkHttpClient httpClient,
            @Value("${images.download-enabled}") boolean downloadEnabled,
            @Value("${images.local-storage-path}") String storagePath) {
        this.httpClient = httpClient;
        this.downloadEnabled = downloadEnabled;
        this.storagePath = storagePath;
        
        if (downloadEnabled) {
            initializeStorageDirectory();
        }
    }
    
    private void initializeStorageDirectory() {
        try {
            Path directory = Paths.get(storagePath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                log.info("Created image storage directory: {}", directory.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create storage directory: {}", storagePath, e);
        }
    }
    
    public String downloadImage(String imageUrl, String topic) {
        ImageDownloadResult result = downloadImageDetailed(imageUrl, topic);
        return result.localPath();
    }
    
    public ImageDownloadResult downloadImageDetailed(String imageUrl, String topic) {
        if (!downloadEnabled) {
            log.debug("Image download disabled, returning original URL");
            return new ImageDownloadResult(imageUrl, null);
        }
        
        try {
            // Generate unique filename
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            String safeTopic = topic.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String filename = String.format("%s_%s_%s.png", timestamp, safeTopic, uniqueId);
            
            Path targetPath = Paths.get(storagePath, filename);
            
            // Download image
            Request request = new Request.Builder()
                    .url(imageUrl)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to download image: HTTP {}", response.code());
                    return new ImageDownloadResult(imageUrl, null); // Return original URL on failure
                }
                
                if (response.body() != null) {
                    Files.write(targetPath, response.body().bytes());
                    log.info("Downloaded image to: {}", targetPath.toAbsolutePath());
                    return new ImageDownloadResult(targetPath.toAbsolutePath().toString(), filename);
                }
            }
            
        } catch (Exception e) {
            log.error("Error downloading image from {}", imageUrl, e);
        }
        
        // Return original URL if download fails
        return new ImageDownloadResult(imageUrl, null);
    }
    
    public record ImageDownloadResult(String localPath, String filename) {}
}