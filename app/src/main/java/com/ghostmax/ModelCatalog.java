package com.ghostmax;
import java.util.*;
public class ModelCatalog {
    public static class ModelEntry {
        public String name, provider, modelId, category, filterType, specialization;
        public boolean needsKey, free;
        public ModelEntry(String name, String provider, String modelId, String category, String filterType, String specialization, boolean needsKey, boolean free) {
            this.name=name; this.provider=provider; this.modelId=modelId; this.category=category; this.filterType=filterType; this.specialization=specialization; this.needsKey=needsKey; this.free=free;
        }
    }
    public static List<ModelEntry> getAll() {
        List<ModelEntry> list = new ArrayList<>();
        list.add(new ModelEntry("Llama 3.3 70B", "OpenRouter", "meta-llama/llama-3.3-70b-instruct:free", "Chat", "No Filter", "Allrounder", true, true));
        list.add(new ModelEntry("Llama 3.3 70B (Groq)", "Groq", "llama-3.3-70b-versatile", "Chat", "No Filter", "Allrounder", true, true));
        list.add(new ModelEntry("GPT-4o", "OpenAI", "gpt-4o", "Chat", "Filtered", "Multimodal", true, false));
        list.add(new ModelEntry("Claude 3.5 Sonnet", "Anthropic", "claude-3-5-sonnet-20241022", "Chat", "Filtered", "Schreiben/Coding", true, false));
        list.add(new ModelEntry("Gemini 2.5 Flash", "Google", "gemini-2.5-flash", "Chat", "Filtered", "Multimodal", true, false));
        list.add(new ModelEntry("DeepSeek V3", "DeepSeek", "deepseek-chat", "Chat", "No Filter", "Coding", true, false));
        list.add(new ModelEntry("GhostGPT", "GhostGPT", "ghostgpt", "Chat", "No Filter", "OpenAI-kompatibel", true, false));
        list.add(new ModelEntry("FreedomGPT", "FreedomGPT", "freedomgpt", "Chat", "No Filter", "Uncensored", false, true));
        return list;
    }
    public static List<ModelEntry> getByFilterType(String filter) { List<ModelEntry> res = new ArrayList<>(); for (ModelEntry e : getAll()) if (e.filterType.equals(filter)) res.add(e); return res; }
}
