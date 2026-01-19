package com.example.chatbot.model;

public class Message {

    public static String SENT_BY_ME = "me";
    public static String SENT_BY_BOT = "bot";

    private long sessionId; // them sessionIf
    private String message;
    private String sentBy;

    private long timestamp;

    public Message(String message, String sentBy, long timestamp) {
        this.message = message;
        this.sentBy = sentBy;
        this.timestamp = timestamp;
    }

    public Message(long sessionId, String message, String sentBy, long timestamp) {
        this.sessionId = sessionId;
        this.message = message;
        this.sentBy = sentBy;
        this.timestamp = timestamp;
    }

    //Chuyển từ MessageEntity sang Message
    public static Message fromEntity(MessageEntity entity) {
        return new Message(entity.sessionId, entity.messages, entity.sentBy, entity.timestamp);
    }

    //Chuyển từ Message sang MessageEntity
    public MessageEntity toEntity(long sessionId) {
        return new MessageEntity(sessionId ,message, sentBy, timestamp);
    }

    public long getSessionId() {
        return sessionId;
    }

    public String getMessage() {
        return message;
    }

    public String getSentBy() {
        return sentBy;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
