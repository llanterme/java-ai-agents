package za.co.digitalcowboy.agents.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${images.local-storage-path}")
    private String localStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Convert relative path to absolute path
        File storageDir = new File(localStoragePath);
        String absolutePath = storageDir.getAbsolutePath();
        
        // Ensure the path ends with a separator for proper URL mapping
        if (!absolutePath.endsWith(File.separator)) {
            absolutePath += File.separator;
        }
        
        registry
            .addResourceHandler("/generated-image/**")
            .addResourceLocations("file:" + absolutePath)
            .setCachePeriod(3600); // Cache for 1 hour
    }
}