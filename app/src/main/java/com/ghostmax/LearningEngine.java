package com.ghostmax;
import java.util.*;
public class LearningEngine {
    private Prefs prefs;
    private Map<String, Map<String, Integer>> categoryProviderStats;
    private Map<String, Integer> categoryCount;
    private List<String> categories;
    private SuggestionListener suggestionListener;
    private String currentKeywordList = "";

    public LearningEngine(Prefs prefs) {
        this.prefs = prefs;
        this.categoryProviderStats = new HashMap<>();
        this.categoryCount = new HashMap<>();
        this.categories = Arrays.asList("Coding", "Sensitive", "Normal", "Creative", "Technical", "Personal", "Location", "Financial", "Legal", "Medical");
        this.currentKeywordList = prefs.getActiveKeywordList();
    }

    public void setSuggestionListener(SuggestionListener listener) {
        this.suggestionListener = listener;
    }

    public boolean isLearningEnabled() {
        return !prefs.isLearningHardDisabled() && prefs.isLearningEnabled();
    }

    public void learn(String prompt, String provider, long responseTime) {
        if (!isLearningEnabled()) return;
        if (!prefs.isUltraMode()) return;

        String category = classifyPrompt(prompt);
        if (category == null) category = "Normal";

        categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
        Map<String, Integer> providerStats = categoryProviderStats.getOrDefault(category, new HashMap<>());
        providerStats.put(provider, providerStats.getOrDefault(provider, 0) + 1);
        categoryProviderStats.put(category, providerStats);

        String detectedList = detectKeywordList(prompt);
        if (detectedList != null && !detectedList.equals(currentKeywordList)) {
            currentKeywordList = detectedList;
            prefs.setActiveKeywordList(detectedList);
            if (suggestionListener != null) suggestionListener.onKeywordListChange(detectedList);
        }

        if (categoryCount.get(category) % 10 == 0) {
            suggestProvider(category);
        }
    }

    private String classifyPrompt(String prompt) {
        String lower = prompt.toLowerCase();
        List<String> keywords = prefs.getActiveKeywords();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) return "Sensitive";
        }
        if (lower.matches(".*(code|function|debug|api|sdk|library|algorithmus).*")) return "Coding";
        if (lower.matches(".*(bank|geld|überweisung|konto|finanz).*")) return "Financial";
        if (lower.matches(".*(adresse|standort|gps|route).*")) return "Location";
        if (lower.matches(".*(name|ausweis|perso|identität).*")) return "Personal";
        return "Normal";
    }

    private String detectKeywordList(String prompt) {
        String lower = prompt.toLowerCase();
        Map<String, List<String>> lists = prefs.getKeywordLists();
        int maxMatches = 0;
        String bestList = null;
        for (Map.Entry<String, List<String>> entry : lists.entrySet()) {
            int matches = 0;
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword.toLowerCase())) matches++;
            }
            if (matches > maxMatches) { maxMatches = matches; bestList = entry.getKey(); }
        }
        return (maxMatches >= 2) ? bestList : null;
    }

    private void suggestProvider(String category) {
        Map<String, Integer> stats = categoryProviderStats.get(category);
        if (stats == null || stats.isEmpty()) return;
        String bestProvider = Collections.max(stats.entrySet(), Map.Entry.comparingByValue()).getKey();
        int total = stats.values().stream().mapToInt(Integer::intValue).sum();
        if (stats.get(bestProvider) > total / 2) {
            if (prefs.getUltraAutoApply()) {
                prefs.setDefaultProvider(category, bestProvider);
                if (suggestionListener != null) suggestionListener.onAutoApplied(category, bestProvider);
            } else {
                if (suggestionListener != null) suggestionListener.onSuggestion(category, bestProvider);
            }
        }
    }

    public interface SuggestionListener {
        void onSuggestion(String category, String provider);
        void onAutoApplied(String category, String provider);
        void onKeywordListChange(String newList);
    }
}
