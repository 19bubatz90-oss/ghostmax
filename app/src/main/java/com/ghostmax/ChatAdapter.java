package com.ghostmax;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    public static final int TYPE_USER = 0, TYPE_BOT = 1, TYPE_SYSTEM = 2;

    public static class DisplayMessage {
        public int type;
        public String text;
        public String meta;
        public boolean isError;
        public int historyIndex = -1;
        public String debugInfo;

        public DisplayMessage(int type, String text, String meta, boolean isError) {
            this.type = type;
            this.text = text;
            this.meta = meta;
            this.isError = isError;
        }
    }

    private List<DisplayMessage> items = new ArrayList<>();
    private int fontMode = 0;

    public void setFontMode(int mode) { this.fontMode = mode; notifyDataSetChanged(); }

    public void add(DisplayMessage m) {
        items.add(m);
        notifyItemInserted(items.size() - 1);
    }

    public void removeLast() {
        if (!items.isEmpty()) {
            int idx = items.size() - 1;
            items.remove(idx);
            notifyItemRemoved(idx);
        }
    }

    public void removeAt(int pos) {
        if (pos >= 0 && pos < items.size()) {
            items.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    public DisplayMessage last() {
        return items.isEmpty() ? null : items.get(items.size() - 1);
    }

    public void clear() {
        int n = items.size();
        items.clear();
        notifyItemRangeRemoved(0, n);
    }

    public int size() { return items.size(); }

    public List<DisplayMessage> getItemsSnapshot() { return new ArrayList<>(items); }

    public void updateLastText(String text) {
        if (!items.isEmpty()) {
            items.get(items.size() - 1).text = text;
            notifyItemChanged(items.size() - 1);
        }
    }

    public void decrementHistoryIndexesAfter(int idx) {
        for (DisplayMessage m : items) {
            if (m.historyIndex > idx) m.historyIndex--;
        }
    }

    public interface OnMessageLongPress {
        void onLongPress(int pos, DisplayMessage msg);
    }
    private OnMessageLongPress longPressListener;
    public void setOnMessageLongPress(OnMessageLongPress l) { this.longPressListener = l; }

    public interface OnMessageClick {
        void onClick(DisplayMessage msg);
    }
    private OnMessageClick clickListener;
    public void setOnMessageClick(OnMessageClick l) { this.clickListener = l; }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int res;
        switch (viewType) {
            case TYPE_USER: res = R.layout.item_message_user; break;
            case TYPE_BOT: res = R.layout.item_message_bot; break;
            default: res = R.layout.item_message_system; break;
        }
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(res, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DisplayMessage m = items.get(position);
        holder.tvText.setText(m.text);

        holder.itemView.setOnLongClickListener(v -> {
            if (longPressListener != null) longPressListener.onLongPress(position, m);
            return true;
        });
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(m);
        });

        if (holder.tvMeta != null) {
            if (m.meta != null && !m.meta.isEmpty()) {
                holder.tvMeta.setVisibility(View.VISIBLE);
                holder.tvMeta.setText(m.meta);
            } else {
                holder.tvMeta.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;
        TextView tvMeta;
        ViewHolder(View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvText);
            tvMeta = itemView.findViewById(R.id.tvMeta);
        }
    }
}
