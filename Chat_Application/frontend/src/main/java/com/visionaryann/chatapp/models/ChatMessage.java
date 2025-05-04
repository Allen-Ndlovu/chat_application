package com.visionaryann.chatapp.models;

public class ChatMessage {
    private String type;
    private String receiver;
    private String content;
    private String room;

    public ChatMessage(String type, String receiver, String content, String room) {
        this.type = type;
        this.receiver = receiver;
        this.content = content;
        this.room = room;
    }

}
