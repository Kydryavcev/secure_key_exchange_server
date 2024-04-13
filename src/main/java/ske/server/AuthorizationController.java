package ske.server;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Server;

import java.security.UnrecoverableKeyException;

public class AuthorizationController
{
    private ServerApplication whatever;
    private Server server;

    @FXML
    private TextField passwordTextField, aliasTextField;

    @FXML
    private Label noAliasLabel, incorrectPasswordLabel;

    @FXML
    protected void onClickLoadKeyButton()
    {
        incorrectPasswordLabel.setVisible(false);
        noAliasLabel.setVisible(false);

        String alias = aliasTextField.getText();
        String password = passwordTextField.getText();

        try
        {
            server.initSignature(alias, password);
        }
        catch (UnrecoverableKeyException ex)
        {
            incorrectPasswordLabel.setVisible(true);
            passwordTextField.setText("");

            return;
        }
        catch (NullPointerException ex)
        {
            noAliasLabel.setVisible(true);

            return;
        }

        whatever.toServerView();
    }

    public void setServer(Server server)
    {
        this.server = server;
    }

    public void setWhatever(ServerApplication whatever)
    {
        this.whatever = whatever;
    }
}
