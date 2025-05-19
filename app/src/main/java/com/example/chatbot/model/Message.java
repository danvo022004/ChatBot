package com.example.chatbot.model;

public class Message {

    public static String SENT_BY_ME = "me";
    public static String SENT_BY_BOT="bot";

   private String message;
   private String sentBy;

   private long timestamp;

    public Message(String message, String sentBy, long timestamp) {
        this.message = message;
        this.sentBy = sentBy;
        this.timestamp = timestamp;
    }

    //Chuyển từ MessageEntity sang Message
    public static Message fromEntity(MessageEntity entity) {
        return new Message(entity.messages, entity.sentBy, entity.timestamp);
    }

    //Chuyển từ Message sang MessageEntity
    public MessageEntity toEntity() {
        return new MessageEntity(message, sentBy, timestamp);
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
