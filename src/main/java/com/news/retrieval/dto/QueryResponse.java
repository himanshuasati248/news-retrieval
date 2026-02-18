package com.news.retrieval.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryResponse {

    private Entities entities;
    private List<String> keyConcepts;
    private String primaryIntent;
    private List<String> secondaryIntents;
    private String searchQuery;
    private Double latitude;
    private Double longitude;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entities {
        private List<String> people;
        private List<String> organizations;
        private List<String> locations;
        private List<String> events;
        private List<String> sources;
        private List<String> categories;
    }


    public List<String> getIntents() {
        List<String> all = new ArrayList<>();
        if (primaryIntent != null && !primaryIntent.isBlank()) {
            all.add(primaryIntent);
        }
        if (secondaryIntents != null) {
            for (String s : secondaryIntents) {
                if (!all.contains(s)) all.add(s);
            }
        }
        return all;
    }

    public String getCategory() {
        if (entities != null && entities.getCategories() != null && !entities.getCategories().isEmpty()) {
            return entities.getCategories().getFirst();
        }
        return null;
    }

    public void setCategory(String category) {
        if (entities == null) entities = new Entities();
        if (entities.getCategories() == null) entities.setCategories(new ArrayList<>());
        entities.getCategories().clear();
        if (category != null) entities.getCategories().add(category);
    }

    public String getSource() {
        if (entities != null && entities.getSources() != null && !entities.getSources().isEmpty()) {
            return entities.getSources().getFirst();
        }
        return null;
    }

    public void setSource(String source) {
        if (entities == null) entities = new Entities();
        if (entities.getSources() == null) entities.setSources(new ArrayList<>());
        entities.getSources().clear();
        if (source != null) entities.getSources().add(source);
    }

}
