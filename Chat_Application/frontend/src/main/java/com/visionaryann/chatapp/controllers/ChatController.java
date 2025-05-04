package com.visionaryann.chatapp.controllers;

import com.chatapp.models.ChatMessage;
import com.google.gson.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class ChatController {
    @FXML private ListView<String> usersList;
    @FXML private ListView<String> messagesList;
    @FXML private TextField messageField;
    @FXML private Label typingLabel;

    private WebSocketClient ws;
    private String token, username;

    public void initSession(String token, String username) {
        this.token = token;
        this.username = username;
        connect();
    }

    private void connect() {
        try {
            URI uri = new URI("ws://localhost:3000?token=" + token);
            ws = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake sh) {
                    send(new Gson().toJson(Map.of("type","JOIN_ROOM","room","global")));
                }
                @Override
                public void onMessage(String msg) {
                    JsonObject obj = JsonParser.parseString(msg).getAsJsonObject();
                    Platform.runLater(() -> handleMessage(obj));
                }
                @Override public void onClose(int c,String r,boolean m){}
                @Override public void onError(Exception e){ e.printStackTrace(); }
            };
            ws.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(JsonObject o) {
        String type = o.get("type").getAsString();
        switch (type) {
            case "USER_LIST":
                usersList.getItems().setAll(
                        Arrays.asList(new Gson().fromJson(o.get("users"), String[].class))
                );
                break;
            case "MESSAGE_HISTORY":
                messagesList.getItems().clear();
                for (JsonElement el : o.getAsJsonArray("messages"))
                    append(el.getAsJsonObject());
                break;
            case "CHAT_MESSAGE":
                append(o);
                break;
            case "TYPING":
                String who = o.get("sender").getAsString();
                typingLabel.setText(who + " is typing...");
                new Timer().schedule(new TimerTask() {
                    @Override public void run() {
                        Platform.runLater(() -> typingLabel.setText(""));
                    }
                }, 2000);
                break;
        }
    }

    private void append(JsonObject m) {
        String time = Instant
                .parse(m.get("timestamp").getAsString())
                .atZone(ZoneId.systemDefault())
                .toLocalTime().withSecond(0).withNano(0).toString();

        messagesList.getItems().add(
                String.format("[%s] %s: %s",
                        time,
                        m.get("sender").getAsString(),
                        m.get("content").getAsString())
        );
    }

    @FXML
    private void onSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        ChatMessage msg = new ChatMessage("CHAT_MESSAGE", null, text, "global");
        ws.send(new Gson().toJson(msg));
        messageField.clear();
    }

    @FXML
    private void onTyping(KeyEvent e) {
        ws.send(new Gson().toJson(Map.of("type","TYPING","room","global")));
    }
}
