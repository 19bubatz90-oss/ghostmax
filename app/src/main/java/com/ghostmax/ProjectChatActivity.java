package com.ghostmax;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
public class ProjectChatActivity extends AppCompatActivity {
    private static final String EXTRA_PROJECT = "project_name";
    private ProjectManager pm;
    private Project currentProject;
    private ChatAdapter chatAdapter;
    private EditText inputText;

    public static void start(Context ctx, String projectName) {
        Intent i = new Intent(ctx, ProjectChatActivity.class);
        i.putExtra(EXTRA_PROJECT, projectName);
        ctx.startActivity(i);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_chat);
        String projectName = getIntent().getStringExtra(EXTRA_PROJECT);
        if (projectName == null) { finish(); return; }
        pm = ProjectManager.getInstance(this);
        currentProject = pm.getProject(projectName);
        if (currentProject == null) { finish(); return; }
        setTitle("Projekt: " + projectName);

        RecyclerView recyclerView = findViewById(R.id.chatRecycler);
        chatAdapter = new ChatAdapter();
        loadMessages();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        inputText = findViewById(R.id.inputText);
        findViewById(R.id.sendBtn).setOnClickListener(v -> {
            String text = inputText.getText().toString().trim();
            if (!text.isEmpty()) {
                ChatMessage msg = new ChatMessage(ChatMessage.TYPE_USER, text, false);
                currentProject.addMessage(msg);
                pm.broadcastMessage(currentProject.getName(), text, ChatMessage.TYPE_SYSTEM);
                loadMessages();
                inputText.setText("");
            }
        });
        findViewById(R.id.subscribeBtn).setOnClickListener(v -> showSubscribeDialog());
        findViewById(R.id.broadcastBtn).setOnClickListener(v -> {
            String text = inputText.getText().toString().trim();
            if (!text.isEmpty()) {
                pm.broadcastMessage(currentProject.getName(), text, ChatMessage.TYPE_ASSISTANT);
                loadMessages();
                inputText.setText("");
            }
        });
    }

    private void loadMessages() {
        chatAdapter.clear();
        for (ChatMessage m : currentProject.getMessages()) {
            ChatAdapter.DisplayMessage dm = new ChatAdapter.DisplayMessage(
                m.type == ChatMessage.TYPE_USER ? ChatAdapter.TYPE_USER :
                m.type == ChatMessage.TYPE_ASSISTANT ? ChatAdapter.TYPE_BOT : ChatAdapter.TYPE_SYSTEM,
                m.text, null, m.isError
            );
            chatAdapter.add(dm);
        }
    }

    private void showSubscribeDialog() {
        List<Project> all = pm.getAllProjects();
        String[] names = new String[all.size()];
        boolean[] checked = new boolean[all.size()];
        for (int i=0; i<all.size(); i++) {
            names[i] = all.get(i).getName();
            checked[i] = currentProject.isSubscribedTo(names[i]);
        }
        new AlertDialog.Builder(this)
                .setTitle("Projekte abonnieren")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    String proj = names[which];
                    if (isChecked) currentProject.subscribeTo(proj);
                    else currentProject.unsubscribeFrom(proj);
                })
                .setPositiveButton("OK", null)
                .show();
    }
}
