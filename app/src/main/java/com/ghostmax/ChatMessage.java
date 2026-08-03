package com.ghostmax;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_ASSISTANT = 1;
    public static final int TYPE_SYSTEM = 2;
    public int type;
    public String text;
    public boolean isError;
    public long timestamp;

    public ChatMessage(int type, String text, boolean isError) {
        this.type = type;
        this.text = text;
        this.isError = isError;
        this.timestamp = System.currentTimeMillis();
    }
}
