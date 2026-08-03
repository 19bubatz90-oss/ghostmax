package com.ghostmax;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
public class ProjectListActivity extends AppCompatActivity {
    private ProjectManager pm;
    private RecyclerView recyclerView;
    private ProjectAdapter adapter;
    private List<Project> projects;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);
        pm = ProjectManager.getInstance(this);
        recyclerView = findViewById(R.id.projectRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.createProjectBtn).setOnClickListener(v -> showCreateProjectDialog());
        loadProjects();
    }

    private void loadProjects() {
        projects = pm.getAllProjects();
        adapter = new ProjectAdapter(projects, getLayoutInflater(), project -> ProjectChatActivity.start(this, project.getName()));
        recyclerView.setAdapter(adapter);
    }

    private void showCreateProjectDialog() {
        EditText input = new EditText(this);
        input.setHint("Projektname");
        new AlertDialog.Builder(this).setTitle("Neues Projekt").setView(input)
                .setPositiveButton("Erstellen", (d,w) -> { String name = input.getText().toString().trim(); if (!name.isEmpty()) { pm.createProject(name); loadProjects(); } })
                .setNegativeButton("Abbrechen", null).show();
    }

    private static class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {
        private List<Project> projects;
        private LayoutInflater inflater;
        private OnItemClickListener listener;
        interface OnItemClickListener { void onItemClick(Project p); }
        ProjectAdapter(List<Project> projects, LayoutInflater inflater, OnItemClickListener listener) {
            this.projects = projects;
            this.inflater = inflater;
            this.listener = listener;
        }
        @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(inflater.inflate(R.layout.item_project, parent, false));
        }
        @Override public void onBindViewHolder(ViewHolder holder, int pos) {
            Project p = projects.get(pos);
            holder.nameView.setText(p.getName());
            holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(p); });
        }
        @Override public int getItemCount() { return projects.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameView;
            ViewHolder(View itemView) { super(itemView); nameView = itemView.findViewById(R.id.projectName); }
        }
    }
}
