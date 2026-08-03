package com.ghostmax;
import java.util.ArrayList;
import java.util.List;
public class Project {
    private String name;
    private List<ChatMessage> messages;
    private List<String> subscribedProjects;

    public Project(String name) {
        this.name = name;
        this.messages = new ArrayList<>();
        this.subscribedProjects = new ArrayList<>();
    }

    public String getName() { return name; }
    public List<ChatMessage> getMessages() { return messages; }
    public List<String> getSubscribedProjects() { return subscribedProjects; }

    public void addMessage(ChatMessage msg) { messages.add(msg); }
    public void subscribeTo(String projectName) {
        if (!subscribedProjects.contains(projectName)) subscribedProjects.add(projectName);
    }
    public void unsubscribeFrom(String projectName) { subscribedProjects.remove(projectName); }
    public boolean isSubscribedTo(String projectName) { return subscribedProjects.contains(projectName); }
}
