package com.visionaryann.chatapp.controllers;

import com.google.gson.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.net.http.*;
import java.net.URI;
import java.util.Map;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private static final String API = "http://localhost:3000/api/auth/login";

    @FXML
    private void onLogin(ActionEvent e) {
        String uname = usernameField.getText().trim();
        String pass  = passwordField.getText().trim();
        if (uname.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Both fields are required.");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            String json = new Gson().toJson(Map.of("username", uname, "password", pass));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonObject jo = JsonParser.parseString(resp.body()).getAsJsonObject();
                String token = jo.get("token").getAsString();
                openChatWindow(token, jo.get("username").getAsString());
            } else {
                errorLabel.setText("Login failed: " + resp.body());
            }
        } catch (Exception ex) {
            errorLabel.setText("Error: " + ex.getMessage());
        }
    }

    private void openChatWindow(String token, String username) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
        Parent root = loader.load();

        ChatController ctrl = loader.getController();
        ctrl.initSession(token, username);

        Stage stage = new Stage();
        stage.setTitle(" Chat — " + username);
        stage.setScene(new Scene(root));
        stage.show();

        ((Stage)usernameField.getScene().getWindow()).close();
    }
}
