package ske.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class ServerApplication extends Application
{
    @Override
    public void start(Stage stage) throws IOException
    {
        FXMLLoader fxmlLoader = new FXMLLoader(ServerApplication.class.getResource("server-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 520);

        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.setTitle("Приложение сервера");
        InputStream inIcon = ServerApplication.class.getResourceAsStream("/image/server.png");
        stage.getIcons().add(new Image(inIcon));
        stage.setScene(scene);
        stage.show();

        ServerController controller = fxmlLoader.getController();

        stage.setOnCloseRequest(controller.getCloseEventHandler());
    }

    public static void main(String[] args)
    {
        launch();
    }

}
