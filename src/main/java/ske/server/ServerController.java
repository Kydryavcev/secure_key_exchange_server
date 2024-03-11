package ske.server;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Server;

public class ServerController
{
    private Server server;

    @FXML
    private TextField portNumberTextField;
    @FXML
    private CheckBox portAutoNumberCheckBox;

    @FXML
    private Label errorValuePortNumberLabel;

    @FXML
    private Button connectionButton;

    @FXML
    protected void switchPortCheckBox()
    {
        if (portNumberTextField.isDisabled())
        {
            portNumberTextField.setDisable(false);

            if (!canConnect())
                connectionButton.setDisable(true);
        }
        else
        {
            portNumberTextField.setDisable(true);

            if (canConnect())
                connectionButton.setDisable(false);
        }
    }

    @FXML
    public void initialize()
    {
        portNumberTextField.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                if (!newValue.matches("[^0]\\d{1,4}|\\d?"))
                {
                    portNumberTextField.setText(oldValue);
                }

                String currentValue = portNumberTextField.getText();

                if (currentValue.length() != 0)
                {
                    int portNumber = 0;

                    portNumber = Integer.parseInt(currentValue);



                    if (portNumber > 65535 )
                    {
                        errorValuePortNumberLabel.setVisible(true);
                    }
                    else
                    {
                        errorValuePortNumberLabel.setVisible(false);
                    }
                }

                if (canConnect())
                    connectionButton.setDisable(false);
                else
                    connectionButton.setDisable(true);
            }
        });
    }

    private boolean canConnect()
    {
        if (portNumberTextField.isDisabled() == true)
            return true;

        String portNumberTextFieldValue = portNumberTextField.getText();

        if (portNumberTextFieldValue.length() == 0)
            return false;

        try
        {
            int portNumber = Integer.parseInt(portNumberTextFieldValue);

            if (portNumber < 0 || portNumber > 65535)
                return false;

            return true;
        }
        catch (NumberFormatException ex)
        {
            return false;
        }
    }
}
