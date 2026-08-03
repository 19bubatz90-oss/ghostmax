package com.ghostmax;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Type;
import java.util.*;

public class Prefs {
    private SharedPreferences sp;
    private Gson gson = new Gson();

    public Prefs(Context ctx) {
        try { sp = CryptoHelper.getEncryptedPrefs(ctx); } catch (Exception e) { sp = ctx.getSharedPreferences("ghostmax_fallback", Context.MODE_PRIVATE); }
    }

    // === Provider Keys – HART CODIERT (Original) ===
    public String getApiKey(String provider) {
        String key = sp.getString("key_" + provider, "");
        if (key.isEmpty()) {
            if (provider.equals("OpenRouter")) key = "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";
            else if (provider.equals("Groq")) key = "gsk_testkey";
            else if (provider.equals("GhostGPT")) key = "test";
            else if (provider.equals("FreedomGPT")) key = "public";
            // Weitere Keys können hier ergänzt werden
        }
        return key;
    }
    public void setApiKey(String provider, String key) { sp.edit().putString("key_" + provider, key).apply(); }

    // === URLs ===
    public String getProviderUrl(String provider) { return sp.getString("url_" + provider, ""); }
    public void setProviderUrl(String provider, String url) { sp.edit().putString("url_" + provider, url).apply(); }

    // === Modelle ===
    public String getProviderModel(String provider) { return sp.getString("model_" + provider, ""); }
    public void setProviderModel(String provider, String model) { sp.edit().putString("model_" + provider, model).apply(); }

    // === System ===
    public String getSystemPrompt() { return sp.getString("system_prompt", "Du bist ein hilfreicher KI-Assistent."); }
    public void setSystemPrompt(String v) { sp.edit().putString("system_prompt", v).apply(); }
    public String getChainCommand() { return sp.getString("chain_command", ""); }
    public void setChainCommand(String v) { sp.edit().putString("chain_command", v).apply(); }

    // === History ===
    public String getHistory() { return sp.getString("chat_history", "[]"); }
    public void saveHistory(String json) { sp.edit().putString("chat_history", json).apply(); }

    // === Temperatur/Tokens ===
    public double getTemperature() { return sp.getFloat("temperature", 0.7f); }
    public void setTemperature(double v) { sp.edit().putFloat("temperature", (float)v).apply(); }
    public int getMaxTokens() { return sp.getInt("max_tokens", 4096); }
    public void setMaxTokens(int v) { sp.edit().putInt("max_tokens", v).apply(); }
    public int getHistoryWindow() { return sp.getInt("history_window", 8); }
    public void setHistoryWindow(int v) { sp.edit().putInt("history_window", v).apply(); }
    public boolean getFallbackEnabled() { return sp.getBoolean("fallback_enabled", true); }
    public void setFallbackEnabled(boolean v) { sp.edit().putBoolean("fallback_enabled", v).apply(); }

    // === Modus ===
    public String getCurrentMode() { return sp.getString("current_mode", "Smart"); }
    public void setCurrentMode(String mode) { sp.edit().putString("current_mode", mode).apply(); }
    public boolean isUltraMode() { return "Ultra".equals(getCurrentMode()); }
    public boolean isNitroMode() { return "Nitro".equals(getCurrentMode()); }
    public boolean isSilentMode() { return "Silent".equals(getCurrentMode()); }
    public boolean isSmartMode() { return "Smart".equals(getCurrentMode()); }

    // === UI ===
    public boolean getDarkMode() { return sp.getBoolean("dark_mode", false); }
    public void setDarkMode(boolean v) { sp.edit().putBoolean("dark_mode", v).apply(); }
    public int getThemeMode() { return sp.getInt("theme_mode", 2); }
    public void setThemeMode(int v) { sp.edit().putInt("theme_mode", v).apply(); }
    public boolean getDebugMode() { return sp.getBoolean("debug_mode", false); }
    public void setDebugMode(boolean v) { sp.edit().putBoolean("debug_mode", v).apply(); }
    public boolean getShowProvider() { return sp.getBoolean("show_provider", true); }
    public void setShowProvider(boolean v) { sp.edit().putBoolean("show_provider", v).apply(); }
    public boolean getAutoScroll() { return sp.getBoolean("auto_scroll", true); }
    public void setAutoScroll(boolean v) { sp.edit().putBoolean("auto_scroll", v).apply(); }
    public boolean getAutoScrollSystem() { return sp.getBoolean("auto_scroll_system", true); }
    public void setAutoScrollSystem(boolean v) { sp.edit().putBoolean("auto_scroll_system", v).apply(); }
    public boolean getShowTimestamps() { return sp.getBoolean("show_timestamps", true); }
    public void setShowTimestamps(boolean v) { sp.edit().putBoolean("show_timestamps", v).apply(); }
    public boolean getShowLatency() { return sp.getBoolean("show_latency", true); }
    public void setShowLatency(boolean v) { sp.edit().putBoolean("show_latency", v).apply(); }
    public boolean getShowTokenCount() { return sp.getBoolean("show_token_count", false); }
    public void setShowTokenCount(boolean v) { sp.edit().putBoolean("show_token_count", v).apply(); }
    public boolean getNotifications() { return sp.getBoolean("notifications", true); }
    public void setNotifications(boolean v) { sp.edit().putBoolean("notifications", v).apply(); }
    public boolean getCopyButton() { return sp.getBoolean("copy_button", true); }
    public void setCopyButton(boolean v) { sp.edit().putBoolean("copy_button", v).apply(); }
    public int getFontSize() { return sp.getInt("font_size", 18); }
    public void setFontSize(int v) { sp.edit().putInt("font_size", v).apply(); }
    public int getBubbleFont() { return sp.getInt("bubble_font", 0); }
    public void setBubbleFont(int v) { sp.edit().putInt("bubble_font", v).apply(); }
    public int getAutoDeleteDays() { return sp.getInt("auto_delete_days", 0); }
    public void setAutoDeleteDays(int v) { sp.edit().putInt("auto_delete_days", v).apply(); }
    public boolean getHapticFeedback() { return sp.getBoolean("haptic_feedback", true); }
    public void setHapticFeedback(boolean v) { sp.edit().putBoolean("haptic_feedback", v).apply(); }
    public boolean getUse24HourFormat() { return sp.getBoolean("use_24h_format", true); }
    public void setUse24HourFormat(boolean v) { sp.edit().putBoolean("use_24h_format", v).apply(); }
    public int getAutoSecretMinutes() { return sp.getInt("auto_secret_minutes", 0); }
    public void setAutoSecretMinutes(int v) { sp.edit().putInt("auto_secret_minutes", v).apply(); }

    // === Favoriten ===
    public List<String> getFavorites() {
        List<String> list = new ArrayList<>();
        try { JSONArray arr = new JSONArray(sp.getString("favorites", "[]")); for (int i=0; i<arr.length(); i++) list.add(arr.getString(i)); } catch (Exception e) {}
        return list;
    }
    public boolean isFavorite(String text) { return getFavorites().contains(text); }
    public void addFavorite(String text) { List<String> list = getFavorites(); if (!list.contains(text)) { list.add(text); saveFavorites(list); } }
    public void removeFavorite(String text) { List<String> list = getFavorites(); list.remove(text); saveFavorites(list); }
    private void saveFavorites(List<String> list) { JSONArray arr = new JSONArray(); for (String s : list) arr.put(s); sp.edit().putString("favorites", arr.toString()).apply(); }
    public int getMaxFavorites() { return sp.getInt("max_favorites", 0); }
    public void setMaxFavorites(int v) { sp.edit().putInt("max_favorites", v).apply(); }

    // === Statistik ===
    public void incrementProviderUsage(String provider) {
        try { JSONObject obj = new JSONObject(sp.getString("provider_usage", "{}")); obj.put(provider, obj.optInt(provider, 0) + 1); sp.edit().putString("provider_usage", obj.toString()).apply(); } catch (Exception e) {}
    }
    public JSONObject getProviderUsage() { try { return new JSONObject(sp.getString("provider_usage", "{}")); } catch (Exception e) { return new JSONObject(); } }

    // === Custom Providers ===
    public List<CustomProvider> getCustomProviders() {
        List<CustomProvider> list = new ArrayList<>();
        try { JSONArray arr = new JSONArray(sp.getString("custom_providers", "[]")); for (int i=0; i<arr.length(); i++) { JSONObject o = arr.getJSONObject(i); list.add(new CustomProvider(o.getString("name"), o.getString("baseUrl"), o.getString("apiKey"), o.getString("model"), o.optString("category"))); } } catch (Exception e) {}
        return list;
    }
    public void addCustomProvider(CustomProvider p) {
        List<CustomProvider> list = getCustomProviders(); list.add(p);
        try { JSONArray arr = new JSONArray(); for (CustomProvider cp : list) { JSONObject o = new JSONObject(); o.put("name", cp.name); o.put("baseUrl", cp.baseUrl); o.put("apiKey", cp.apiKey); o.put("model", cp.model); o.put("category", cp.category); arr.put(o); } sp.edit().putString("custom_providers", arr.toString()).apply(); } catch (Exception e) {}
    }
    public void removeCustomProvider(String name) {
        List<CustomProvider> list = getCustomProviders(); list.removeIf(p -> p.name.equals(name));
        try { JSONArray arr = new JSONArray(); for (CustomProvider cp : list) { JSONObject o = new JSONObject(); o.put("name", cp.name); o.put("baseUrl", cp.baseUrl); o.put("apiKey", cp.apiKey); o.put("model", cp.model); o.put("category", cp.category); arr.put(o); } sp.edit().putString("custom_providers", arr.toString()).apply(); } catch (Exception e) {}
    }
    public void updateCustomProvider(CustomProvider p) {
        List<CustomProvider> list = getCustomProviders();
        for (int i=0; i<list.size(); i++) { if (list.get(i).name.equals(p.name)) { list.set(i, p); break; } }
        try { JSONArray arr = new JSONArray(); for (CustomProvider cp : list) { JSONObject o = new JSONObject(); o.put("name", cp.name); o.put("baseUrl", cp.baseUrl); o.put("apiKey", cp.apiKey); o.put("model", cp.model); o.put("category", cp.category); arr.put(o); } sp.edit().putString("custom_providers", arr.toString()).apply(); } catch (Exception e) {}
    }

    // === Persönlichkeit ===
    public String getPersonality() { return sp.getString("personality", "Freundlicher Assistent"); }
    public void setPersonality(String v) { sp.edit().putString("personality", v).apply(); }
    public String getCustomPersonalities() { return sp.getString("custom_personalities", "[]"); }
    public void saveCustomPersonalities(String json) { sp.edit().putString("custom_personalities", json).apply(); }

    // === Onboarding ===
    public boolean getOnboardingShown() { return sp.getBoolean("onboarding_shown", false); }
    public void setOnboardingShown(boolean v) { sp.edit().putBoolean("onboarding_shown", v).apply(); }
    public boolean getKeysInitialized() { return sp.getBoolean("keys_initialized", false); }
    public void setKeysInitialized(boolean v) { sp.edit().putBoolean("keys_initialized", v).apply(); }

    // === Letztes Modell ===
    public String getLastModelName() { return sp.getString("last_model_name", ""); }
    public void setLastModelName(String v) { sp.edit().putString("last_model_name", v).apply(); }

    // === Meta-Reihenfolge ===
    public int[] getMetaOrder() {
        String raw = sp.getString("meta_order", "1,2,3,4");
        try { String[] parts = raw.split(","); int[] out = new int[4]; for (int i=0; i<4; i++) out[i] = Integer.parseInt(parts[i].trim()); return out; } catch (Exception e) { return new int[]{1,2,3,4}; }
    }
    public void setMetaOrder(int a, int b, int c, int d) { sp.edit().putString("meta_order", a+","+b+","+c+","+d).apply(); }

    // === Lernen ===
    public boolean isLearningEnabled() { return sp.getBoolean("learning_enabled", true); }
    public void setLearningEnabled(boolean v) { sp.edit().putBoolean("learning_enabled", v).apply(); }
    public boolean isLearningHardDisabled() { return sp.getBoolean("learning_hard_disabled", false); }
    public void setLearningHardDisabled(boolean v) { sp.edit().putBoolean("learning_hard_disabled", v).apply(); }
    public boolean getUltraAutoApply() { return sp.getBoolean("ultra_auto_apply", false); }
    public void setUltraAutoApply(boolean v) { sp.edit().putBoolean("ultra_auto_apply", v).apply(); }

    // === Keyword-Listen ===
    public Map<String, List<String>> getKeywordLists() {
        String json = sp.getString("keyword_lists", "");
        if (json.isEmpty()) return getDefaultKeywordLists();
        Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
        return gson.fromJson(json, type);
    }
    public void saveKeywordLists(Map<String, List<String>> lists) {
        String json = gson.toJson(lists);
        sp.edit().putString("keyword_lists", json).apply();
    }
    public String getActiveKeywordList() { return sp.getString("active_keyword_list", "Coding"); }
    public void setActiveKeywordList(String name) { sp.edit().putString("active_keyword_list", name).apply(); }
    public List<String> getActiveKeywords() {
        Map<String, List<String>> lists = getKeywordLists();
        String active = getActiveKeywordList();
        return lists.getOrDefault(active, new ArrayList<>());
    }
    public void setDefaultProvider(String category, String provider) {
        sp.edit().putString("default_provider_" + category, provider).apply();
    }
    public String getDefaultProvider(String category) {
        return sp.getString("default_provider_" + category, "");
    }

    private Map<String, List<String>> getDefaultKeywordLists() {
        Map<String, List<String>> lists = new LinkedHashMap<>();
        lists.put("Coding", Arrays.asList("Code", "API", "SDK", "Library", "Framework", "Algorithmus", "Datenbank", "SQL", "Debug", "Function", "Variable", "Klasse", "Objekt"));
        lists.put("News", Arrays.asList("Bericht", "Quelle", "Interview", "Pressekonferenz", "Politik", "Wirtschaft", "Enthüllung", "Skandal", "Korruption"));
        lists.put("Darkweb", Arrays.asList("Tor", "Onion", "Darknet", "Bitcoin", "Monero", "Anonym", "Marktplatz", "Hacking", "Malware"));
        lists.put("Wahrheit hinter News", Arrays.asList("Fake", "Propaganda", "Desinformation", "Zensur", "Manipulation", "Deep Fake", "Filterblase", "Verschwörung"));
        return lists;
    }
}
