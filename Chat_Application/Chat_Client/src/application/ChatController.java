package application;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import javax.websocket.*;

@ClientEndpoint
public class ChatController {
    @FXML private TextArea chatArea;
    @FXML private TextField inputField;
    private Session session;

    public void initialize() {
        try {
            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/config.properties"));
            String serverUrl = props.getProperty("server.url");

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(serverUrl));
        } catch (Exception e) {
            chatArea.appendText("Failed to connect: " + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message) {
        chatArea.appendText("Server: " + message + "\n");
    }

    @FXML
    public void sendMessage() {
        try {
            String message = inputField.getText();
            session.getBasicRemote().sendText(message);
            chatArea.appendText("You: " + message + "\n");
            inputField.clear();
        } catch (IOException e) {
            chatArea.appendText("Error sending message.\n");
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        chatArea.appendText("Connected to server.\n");
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        chatArea.appendText("Disconnected: " + reason.getReasonPhrase() + "\n");
    }
}
