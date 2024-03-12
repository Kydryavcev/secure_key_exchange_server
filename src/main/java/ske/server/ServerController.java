package ske.server;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Server;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ServerController
{
    public static final int ERROR_PORT_NUMBER = -1;

    private Server server;

    @FXML
    private ProgressBar connectionProgressBar;
    @FXML
    private TextField portNumberTextField;
    @FXML
    private CheckBox portAutoNumberCheckBox;

    @FXML
    private Label errorValuePortNumberLabel, errorConnectionLabel;

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
    protected void onConnectionButton()
    {
        connectionButton.setDisable(true);
        portNumberTextField.setDisable(true);
        portAutoNumberCheckBox.setDisable(true);

        errorConnectionLabel.setVisible(false);

        if (portAutoNumberCheckBox.isSelected())
        {
            try
            {
                server = new Server(0);
            }
            catch (SocketException ex)
            {
                errorConnectionLabel.setText("В базовом протоколе возникла ошибка.");

                errorConnectionLabel.setVisible(true);

                connectionButton.setDisable(!canConnect());
                portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                portAutoNumberCheckBox.setDisable(false);
            }
            catch (IOException ex)
            {
                errorConnectionLabel.setText("При открытии сокета возникла ошибка ввода-вывода.");

                errorConnectionLabel.setVisible(true);

                connectionButton.setDisable(!canConnect());
                portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                portAutoNumberCheckBox.setDisable(false);
            }
            catch (SecurityException ex)
            {
                errorConnectionLabel.setText("Менеджер безопасности и его метод checkListen не разрешает операцию.");

                errorConnectionLabel.setVisible(true);
                connectionButton.setDisable(!canConnect());
                portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                portAutoNumberCheckBox.setDisable(false);
            }
        }
        else
        {
            int portNumber = getPortNumberTextFieldValue();

            if (portNumber == ERROR_PORT_NUMBER)
            {
                errorConnectionLabel.setText("Указан некорректный номер порта. Введите номер порта заново.");

                errorConnectionLabel.setVisible(true);

                connectionButton.setDisable(!canConnect());
                portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                portAutoNumberCheckBox.setDisable(false);
            }

            try
            {
                server = new Server(portNumber);
            }
            catch (SocketException ex)
            {
                errorConnectionLabel.setText("В базовом протоколе возникла ошибка.");

                connectionButton.setDisable(true);

                connectionButton.setDisable(!canConnect());
                portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                portAutoNumberCheckBox.setDisable(false);
            }
            catch (IOException ex)
            {
                errorConnectionLabel.setText("При открытии сокета возникла ошибка ввода-вывода.");

                connectionButton.setDisable(true);

                connectionButton.setDisable(!canConnect());
                portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                portAutoNumberCheckBox.setDisable(false);
            }
            catch (IllegalArgumentException ex)
            {
                errorConnectionLabel.setText("Указан некорректный номер порта. Введите номер порта заново.");

                errorConnectionLabel.setVisible(true);

                connectionButton.setDisable(!canConnect());
                portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                portAutoNumberCheckBox.setDisable(false);
            }
            catch (SecurityException ex)
            {
                errorConnectionLabel.setText("Менеджер безопасности и его метод checkListen не разрешает операцию.");

                errorConnectionLabel.setVisible(true);

                connectionButton.setDisable(!canConnect());
                portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                portAutoNumberCheckBox.setDisable(false);
            }
        }

        Thread connection = new Thread(new ConnectionServer());
        Thread progressBarThread = new Thread(new ProgressBarValue());

        connection.start();
        progressBarThread.start();
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

                boolean visible = (getPortNumberTextFieldValue() == ERROR_PORT_NUMBER);

                errorValuePortNumberLabel.setVisible(visible);

                connectionButton.setDisable(!canConnect());
            }
        });
    }

    private boolean canConnect()
    {
        if (server != null && server.isConnect())
            return false;

        if (portAutoNumberCheckBox.isSelected())
            return true;

        return getPortNumberTextFieldValue() != ERROR_PORT_NUMBER;
    }

    private int getPortNumberTextFieldValue()
    {
        String portNumberTextFieldValue = portNumberTextField.getText();

        try
        {
            int portNumber = Integer.parseInt(portNumberTextFieldValue);

            if (portNumber < 0 || portNumber > 65535)
                new NumberFormatException();

            return portNumber;
        }
        catch (NumberFormatException ex)
        {
            return ERROR_PORT_NUMBER;
        }
    }

    class ConnectionServer implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                server.connection();
            }
            catch (SocketTimeoutException e)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        errorConnectionLabel.setText("Превышено время ожидания.");

                        errorConnectionLabel.setVisible(true);

                        connectionButton.setDisable(!canConnect());
                        portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                        portAutoNumberCheckBox.setDisable(false);
                    }
                });
            }
            catch (IOException ex)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        errorConnectionLabel.setText("Во время ожидания соединения возникла ошибка ввода-вывода.");

                        connectionButton.setDisable(true);

                        connectionButton.setDisable(!canConnect());
                        portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                        portAutoNumberCheckBox.setDisable(false);
                    }
                });
            }
        }
    }

    class ProgressBarValue implements Runnable
    {
        @Override
        public void run()
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    connectionProgressBar.setVisible(true);
                }
            });

            double progressValue = 1;

            while (progressValue > 0)
            {
                double finalProgressValue = progressValue;
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        connectionProgressBar.setProgress(finalProgressValue);
                    }
                });


                progressValue -= 0.01;

                try
                {
                    Thread.sleep(95);
                }
                catch (InterruptedException e)
                {
                }
            }

            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    connectionProgressBar.setVisible(false);
                }
            });
        }
    }
}
