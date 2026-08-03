package com.ghostmax;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Base64;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    public static class Result {
        public String text, actualProvider;
        public boolean isError, isImage;
        public byte[] imageBytes;
        public long latency;

        public Result(String text, String actualProvider, boolean isError) {
            this.text = text;
            this.actualProvider = actualProvider;
            this.isError = isError;
            this.isImage = false;
        }
    }

    private static final Map<String, ProviderInfo> PROVIDERS = new LinkedHashMap<>();
    static {
        PROVIDERS.put("OpenRouter", new ProviderInfo("https://openrouter.ai/api/v1/chat/completions", "meta-llama/llama-3.3-70b-instruct:free"));
        PROVIDERS.put("Groq", new ProviderInfo("https://api.groq.com/openai/v1/chat/completions", "llama-3.3-70b-versatile"));
        PROVIDERS.put("OpenAI", new ProviderInfo("https://api.openai.com/v1/chat/completions", "gpt-4o"));
        PROVIDERS.put("Anthropic", new ProviderInfo("https://api.anthropic.com/v1/messages", "claude-3-5-sonnet-20241022"));
        PROVIDERS.put("Google", new ProviderInfo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent", "gemini-2.5-flash"));
        PROVIDERS.put("DeepSeek", new ProviderInfo("https://api.deepseek.com/v1/chat/completions", "deepseek-chat"));
        PROVIDERS.put("Mistral", new ProviderInfo("https://api.mistral.ai/v1/chat/completions", "mistral-large-latest"));
        PROVIDERS.put("Cohere", new ProviderInfo("https://api.cohere.ai/compatibility/v1/chat/completions", "command-r"));
        PROVIDERS.put("Perplexity", new ProviderInfo("https://api.perplexity.ai/chat/completions", "llama-3.1-sonar-small-128k-online"));
        PROVIDERS.put("Cerebras", new ProviderInfo("https://api.cerebras.ai/v1/chat/completions", "llama3.1-8b"));
        PROVIDERS.put("Venice", new ProviderInfo("https://api.venice.ai/api/v1/chat/completions", "zai-org-glm-5-1"));
        PROVIDERS.put("FreedomGPT", new ProviderInfo("https://api.freedomgpt.com/v1/chat/completions", "freedomgpt"));
        PROVIDERS.put("GhostGPT", new ProviderInfo("https://api.ghostgpt.io/v1/chat/completions", "ghostgpt"));
    }

    private static class ProviderInfo {
        String url, model;
        ProviderInfo(String url, String model) { this.url = url; this.model = model; }
    }

    public static String[] getAllProviders() {
        return PROVIDERS.keySet().toArray(new String[0]);
    }

    public static Result callProvider(String providerName, Prefs prefs, List<ChatMessage> history,
                                      String userText, String systemPrompt, String chainCommand) {
        ProviderInfo info = PROVIDERS.get(providerName);
        if (info == null) return new Result("Provider nicht gefunden: " + providerName, providerName, true);
        String key = prefs.getApiKey(providerName);
        try {
            String fullSysPrompt = (chainCommand != null && !chainCommand.isEmpty())
                    ? chainCommand + "\n\n" + systemPrompt : systemPrompt;
            return callOpenAICompatible(info.url, key, info.model, fullSysPrompt, history, userText, providerName);
        } catch (Exception e) {
            return new Result("Fehler: " + e.getMessage(), providerName, true);
        }
    }

    private static Result callOpenAICompatible(String url, String key, String model,
                                               String systemPrompt, List<ChatMessage> history,
                                               String userText, String providerName) throws Exception {
        JSONObject body = new JSONObject();
        JSONArray messages = new JSONArray();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            messages.put(sys);
        }
        for (ChatMessage m : history) {
            if (m.isError) continue;
            JSONObject msg = new JSONObject();
            msg.put("role", m.type == ChatMessage.TYPE_USER ? "user" : "assistant");
            msg.put("content", m.text);
            messages.put(msg);
        }
        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", userText);
        messages.put(user);

        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 4096);

        Request.Builder req = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), body.toString()));
        if (key != null && !key.isEmpty()) {
            req.header("Authorization", "Bearer " + key);
        }

        try (Response resp = client.newCall(req.build()).execute()) {
            String bodyStr = resp.body().string();
            if (!resp.isSuccessful()) {
                return new Result("HTTP " + resp.code() + ": " + bodyStr, providerName, true);
            }
            JSONObject obj = new JSONObject(bodyStr);
            String content = obj.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
            return new Result(content, providerName, false);
        }
    }

    public static Result callWithFallback(String provider, Prefs prefs, List<ChatMessage> history,
                                          String userText, String category, String chainCommand) {
        Result first = callProvider(provider, prefs, history, userText, prefs.getSystemPrompt(), chainCommand);
        if (!first.isError) return first;
        for (String p : PROVIDERS.keySet()) {
            if (p.equals(provider)) continue;
            Result alt = callProvider(p, prefs, history, userText, prefs.getSystemPrompt(), chainCommand);
            if (!alt.isError) return alt;
        }
        return first;
    }

    // ========================================================================
    // BILDGENERIERUNG
    // ========================================================================
    public static Result generateImage(String prompt, String provider, Prefs prefs) {
        long start = System.currentTimeMillis();
        try {
            if ("OpenAI".equals(provider)) {
                return generateDalle3(prompt, prefs.getApiKey("OpenAI"));
            } else if ("Google".equals(provider)) {
                return generateImagen3(prompt, prefs.getApiKey("Google"));
            } else if ("StableDiffusion".equals(provider)) {
                return generateStableDiffusion(prompt);
            } else {
                return new Result("Bildgenerierung für " + provider + " nicht implementiert. Nutze: OpenAI, Google oder StableDiffusion", provider, true);
            }
        } catch (Exception e) {
            Result r = new Result("Fehler: " + e.getMessage(), provider, true);
            r.latency = System.currentTimeMillis() - start;
            return r;
        }
    }

    private static Result generateDalle3(String prompt, String key) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", "dall-e-3");
        body.put("prompt", prompt);
        body.put("n", 1);
        body.put("size", "1024x1024");
        body.put("response_format", "b64_json");

        Request req = new Request.Builder()
                .url("https://api.openai.com/v1/images/generations")
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), body.toString()))
                .build();

        try (Response resp = client.newCall(req).execute()) {
            String bodyStr = resp.body().string();
            if (!resp.isSuccessful()) {
                return new Result("HTTP " + resp.code() + ": " + bodyStr, "OpenAI", true);
            }
            JSONObject obj = new JSONObject(bodyStr);
            JSONArray data = obj.getJSONArray("data");
            if (data.length() == 0) {
                return new Result("Kein Bild erhalten", "OpenAI", true);
            }
            String b64 = data.getJSONObject(0).getString("b64_json");
            Result r = new Result("🖼️ Bild erzeugt (DALL-E 3)", "OpenAI", false);
            r.isImage = true;
            r.imageBytes = Base64.decode(b64, Base64.DEFAULT);
            return r;
        }
    }

    private static Result generateImagen3(String prompt, String key) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-002:predict?key=" + key;
        JSONObject instance = new JSONObject();
        instance.put("prompt", prompt);
        JSONObject parameters = new JSONObject();
        parameters.put("sampleCount", 1);
        parameters.put("aspectRatio", "1:1");
        JSONObject body = new JSONObject();
        body.put("instances", new JSONArray().put(instance));
        body.put("parameters", parameters);

        Request req = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), body.toString()))
                .build();

        try (Response resp = client.newCall(req).execute()) {
            String bodyStr = resp.body().string();
            if (!resp.isSuccessful()) {
                return new Result("HTTP " + resp.code() + ": " + bodyStr, "Google", true);
            }
            JSONObject obj = new JSONObject(bodyStr);
            JSONArray predictions = obj.optJSONArray("predictions");
            if (predictions == null || predictions.length() == 0) {
                return new Result("Kein Bild erhalten", "Google", true);
            }
            String b64 = predictions.getJSONObject(0).getString("bytesBase64Encoded");
            Result r = new Result("🖼️ Bild erzeugt (Imagen 3)", "Google", false);
            r.isImage = true;
            r.imageBytes = Base64.decode(b64, Base64.DEFAULT);
            return r;
        }
    }

    private static Result generateStableDiffusion(String prompt) throws Exception {
        String url = "https://api-inference.huggingface.co/models/runwayml/stable-diffusion-v1-5";
        Request req = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), new JSONObject().put("inputs", prompt).toString()))
                .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                return new Result("Stable Diffusion nicht verfügbar (Rate-Limit). Nutze OpenAI oder Google.", "StableDiffusion", true);
            }
            byte[] imageBytes = resp.body().bytes();
            Result r = new Result("🖼️ Bild erzeugt (Stable Diffusion)", "StableDiffusion", false);
            r.isImage = true;
            r.imageBytes = imageBytes;
            return r;
        }
    }
}
