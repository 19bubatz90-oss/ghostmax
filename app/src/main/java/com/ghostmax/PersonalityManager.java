package com.ghostmax;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
public class PersonalityManager {
    public static class Personality {
        public String name, description, systemPrompt, provider, icon;
        public double temperature; public int maxTokens;
        public Personality(String name, String description, String systemPrompt, String provider, double temperature, int maxTokens, String icon) {
            this.name=name; this.description=description; this.systemPrompt=systemPrompt; this.provider=provider; this.temperature=temperature; this.maxTokens=maxTokens; this.icon=icon;
        }
        public JSONObject toJson() { try { JSONObject o = new JSONObject(); o.put("name", name); o.put("description", description); o.put("systemPrompt", systemPrompt); o.put("provider", provider); o.put("temperature", temperature); o.put("maxTokens", maxTokens); o.put("icon", icon); return o; } catch (Exception e) { return new JSONObject(); } }
        public static Personality fromJson(JSONObject o) { try { return new Personality(o.getString("name"), o.getString("description"), o.getString("systemPrompt"), o.getString("provider"), o.getDouble("temperature"), o.getInt("maxTokens"), o.getString("icon")); } catch (Exception e) { return null; } }
    }
    public static List<Personality> getDefaultPersonalities() {
        List<Personality> list = new ArrayList<>();
        list.add(new Personality("Freundlicher Assistent", "Hilfreich", "Du bist ein hilfsbereiter Assistent.", "OpenRouter", 0.7, 1024, "😊"));
        list.add(new Personality("Coding-Profi", "Präzise", "Du bist ein erfahrener Entwickler.", "Groq", 0.3, 2048, "💻"));
        list.add(new Personality("Kreativer Kopf", "Kreativ", "Du bist ein kreativer Geist.", "OpenRouter", 1.2, 1536, "🎨"));
        list.add(new Personality("Philosophischer Denker", "Tiefgründig", "Du bist ein weiser Philosoph.", "Gemini", 1.0, 1024, "🧠"));
        list.add(new Personality("Kritischer Hinterfrager", "Skeptisch", "Du hinterfragst alles kritisch.", "Mistral", 0.5, 1024, "🔍"));
        list.add(new Personality("Humorvoller Entertainer", "Witzig", "Du bringst den Nutzer zum Lachen.", "OpenRouter", 1.5, 1024, "😂"));
        return list;
    }
    public static List<Personality> loadCustom(Prefs prefs) {
        List<Personality> list = new ArrayList<>();
        try { JSONArray arr = new JSONArray(prefs.getCustomPersonalities()); for (int i=0; i<arr.length(); i++) { Personality p = fromJson(arr.getJSONObject(i)); if (p != null) list.add(p); } } catch (Exception e) {}
        return list;
    }
    public static void saveCustom(List<Personality> list, Prefs prefs) {
        JSONArray arr = new JSONArray(); for (Personality p : list) arr.put(p.toJson()); prefs.saveCustomPersonalities(arr.toString());
    }
    public static Personality findByName(String name, Prefs prefs) {
        for (Personality p : getDefaultPersonalities()) if (p.name.equals(name)) return p;
        for (Personality p : loadCustom(prefs)) if (p.name.equals(name)) return p;
        return getDefaultPersonalities().get(0);
    }
}
