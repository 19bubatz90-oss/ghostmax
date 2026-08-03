package com.ghostmax;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;
public class ProjectManager {
    private static ProjectManager instance;
    private Map<String, Project> projects;
    private Context context;
    private static final String PREFS_NAME = "project_manager";
    private static final String KEY_PROJECTS = "projects";

    private ProjectManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        loadProjects();
    }

    public static synchronized ProjectManager getInstance(Context ctx) {
        if (instance == null) instance = new ProjectManager(ctx);
        return instance;
    }

    public Project createProject(String name) {
        if (projects.containsKey(name)) return null;
        Project p = new Project(name);
        projects.put(name, p);
        saveProjects();
        return p;
    }

    public Project getProject(String name) { return projects.get(name); }
    public List<Project> getAllProjects() { return new ArrayList<>(projects.values()); }
    public void deleteProject(String name) { projects.remove(name); saveProjects(); }

    public void sendMessageTo(String from, String to, String text, int type) {
        Project sender = projects.get(from);
        Project receiver = projects.get(to);
        if (sender == null || receiver == null) return;
        ChatMessage msg = new ChatMessage(type, text, false);
        sender.addMessage(msg);
        if (receiver.isSubscribedTo(from)) receiver.addMessage(msg);
        saveProjects();
    }

    public void broadcastMessage(String from, String text, int type) {
        Project sender = projects.get(from);
        if (sender == null) return;
        ChatMessage msg = new ChatMessage(type, text, false);
        sender.addMessage(msg);
        for (Project p : projects.values()) {
            if (p.isSubscribedTo(from)) p.addMessage(msg);
        }
        saveProjects();
    }

    private void saveProjects() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<String, Project> entry : projects.entrySet()) {
            String key = entry.getKey();
            Project p = entry.getValue();
            Map<String, Object> projData = new HashMap<>();
            List<Map<String, Object>> msgList = new ArrayList<>();
            for (ChatMessage m : p.getMessages()) {
                Map<String,Object> msgMap = new HashMap<>();
                msgMap.put("type", m.type);
                msgMap.put("text", m.text);
                msgMap.put("isError", m.isError);
                msgMap.put("timestamp", m.timestamp);
                msgList.add(msgMap);
            }
            projData.put("messages", msgList);
            projData.put("subscribed", p.getSubscribedProjects());
            data.put(key, projData);
        }
        String json = gson.toJson(data);
        prefs.edit().putString(KEY_PROJECTS, json).apply();
    }

    private void loadProjects() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_PROJECTS, "");
        projects = new HashMap<>();
        if (json.isEmpty()) return;
        try {
            Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
            Map<String, Map<String, Object>> data = gson.fromJson(json, type);
            for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
                String name = entry.getKey();
                Project p = new Project(name);
                Map<String, Object> projData = entry.getValue();
                List<Map<String, Object>> msgList = (List<Map<String, Object>>) projData.get("messages");
                if (msgList != null) {
                    for (Map<String,Object> msgMap : msgList) {
                        int t = ((Number)msgMap.get("type")).intValue();
                        String text = (String)msgMap.get("text");
                        boolean err = (boolean)msgMap.get("isError");
                        long ts = ((Number)msgMap.get("timestamp")).longValue();
                        ChatMessage msg = new ChatMessage(t, text, err);
                        msg.timestamp = ts;
                        p.addMessage(msg);
                    }
                }
                List<String> subs = (List<String>) projData.get("subscribed");
                if (subs != null) for (String s : subs) p.subscribeTo(s);
                projects.put(name, p);
            }
        } catch (Exception e) { projects = new HashMap<>(); }
    }
}
