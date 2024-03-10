module ske.server {
    requires javafx.controls;
    requires javafx.fxml;


    opens ske.server to javafx.fxml;
    exports ske.server;
}