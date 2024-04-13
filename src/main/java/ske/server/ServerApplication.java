package ske.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import model.Server;

import java.io.IOException;
import java.io.InputStream;

public class ServerApplication extends Application
{
    private Stage mainStage;
    private Server server = new Server();

    @Override
    public void start(Stage stage) throws IOException
    {
        mainStage = stage;

        FXMLLoader fxmlLoader = new FXMLLoader(ServerApplication.class.getResource("authorization-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        AuthorizationController controller = fxmlLoader.getController();

        controller.setServer(server);
        controller.setWhatever(this);

        mainStage.setTitle("Приложение сервера");
        InputStream inIcon = ServerApplication.class.getResourceAsStream("/image/server.png");
        mainStage.getIcons().add(new Image(inIcon));
        mainStage.setScene(scene);
        mainStage.show();
    }

    public void toServerView()
    {
        FXMLLoader fxmlLoader = new FXMLLoader(ServerApplication.class.getResource("server-view.fxml"));

        try
        {
            Scene scene = new Scene(fxmlLoader.load(), 800, 520);
            mainStage.setScene(scene);
            mainStage.show();
        }
        catch (IOException ex)
        {
            System.out.println(ex.getMessage());
            System.out.println(ex.getClass());
        }

        ServerController controller = fxmlLoader.getController();

        mainStage.setOnCloseRequest(controller.getCloseEventHandler());
        controller.setServer(server);
    }

    public static void main(String[] args)
    {
        launch();
    }
}
