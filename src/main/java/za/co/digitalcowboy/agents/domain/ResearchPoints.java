package za.co.digitalcowboy.agents.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ResearchPoints(
    @JsonProperty("points")
    List<String> points,
    
    @JsonProperty("sources")
    List<String> sources
) {
    public ResearchPoints {
        if (points == null) {
            points = List.of();
        }
        if (sources == null) {
            sources = List.of();
        }
    }
    
    public static ResearchPoints empty() {
        return new ResearchPoints(List.of(), List.of());
    }
}