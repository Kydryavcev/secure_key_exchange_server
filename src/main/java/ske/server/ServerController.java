package ske.server;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.WindowEvent;
import model.Server;

import java.io.IOException;

/**
 * <h2>Контроллер для (за) FXML-файлом</h2>
 *
 * <p>В данном классе происходит обработка событий в графическом интерфейсе. Реализована логика взаимодействия с
 * пользователем, управление логикой сервера.</p>
 *
 * @author Kydryavcev Ilya
 * @version 1.0
 * @since 12.03.24
 */
public class ServerController
{
    /**
     * ОЖИДАНИЕ
     */
    private static final int WAITING = 1;

    private static final int SEND_MESSAGE = 2;
    private static final int MESSAGE_DELIVERED = 3;
    private static final int DISCONNECT = -1;

    private int SIGNAL = WAITING;

    private static final int ERROR_PORT_NUMBER = -1;

    private boolean connected = false;

    private Server server;

    @FXML
    private ProgressBar connectionProgressBar;
    @FXML
    private TextField portNumberTextField;
    @FXML
    private TextArea messageTextArea;
    @FXML
    private CheckBox portAutoNumberCheckBox;

    @FXML
    private Label errorValuePortNumberLabel, errorConnectionLabel, connectionEstablishedLabel,
            connectionNotSecureLabel, errorSendMessageLabel, disconnectLabel;

    @FXML
    private Button connectionButton, disconnectButton, sendMessageButton;

    public void setServer(Server  server)
    {
         this.server = server;
    }

    private javafx.event.EventHandler<WindowEvent> closeEventHandler =
            new javafx.event.EventHandler<WindowEvent>()
            {
                @Override
                public void handle(WindowEvent event)
                {
                    if (server.getSocket() != null)
                            server.disconnect();
                }
            };

    public javafx.event.EventHandler<WindowEvent> getCloseEventHandler()
    {
        return closeEventHandler;
    }

    /**
     * <h2>Переключатель режима выбора порта для соединения.</h2>
     *
     * <p>При активном чекбоксе {@code portAutoNumberCheckBox} порт задаётся автоматически. В выключенном положении
     * номер порта задаётся пользователем.</p>
     */
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

    /**
     * <h3>Обработка нажатия на кнопку "Подключение" - {@code connectionButton}</h3>
     *
     * <p>Данным методом производится попытка наладить соединение с клиент-приложением, а также защитить канал передачи
     * данных.</p>
     *
     *
     * <p>В случае успеха у пользователя появляется возможность отправить сообщение клиент-приложениею</p>
     *
     * <p>В случае неудачи выводиться текст ошибки. Разблокируется контроллеры для настройки соединения (чекбокс, поле
     * ввода).</p>
     */
    @FXML
    protected void onConnectionButton()
    {
        messageTextArea.setDisable(true);
        connectionButton.setDisable(true);
        sendMessageButton.setDisable(true);
        portNumberTextField.setDisable(true);
        portAutoNumberCheckBox.setDisable(true);

        disconnectLabel.setVisible(false);
        errorConnectionLabel.setVisible(false);
        connectionNotSecureLabel.setVisible(false);
        connectionEstablishedLabel.setVisible(false);

        try
        {
            int portNumber = getPortNumberTextFieldValue();

            if (portAutoNumberCheckBox.isSelected())
                server.highlightSocket(0);
            else
                server.highlightSocket(portNumber);
        }
        catch (Server.HighlightSocketException ex)
        {
            errorConnectionLabel.setText(ex.getMessage());

            errorConnectionLabel.setVisible(true);

            connectionButton.setDisable(!canConnect());
            portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
            portAutoNumberCheckBox.setDisable(false);

            return;
        }

        Thread connection = new Thread(new ConnectionServer());
        Thread progressBarThread = new Thread(new ProgressBarValue());

        connection.setDaemon(true);
        progressBarThread.setDaemon(true);

        connection.start();
        progressBarThread.start();
    }

    @FXML
    protected void onClickDisconnectButton()
    {
        server.disconnect();

        connected = false;
    }

    /**
     * <h3>Обработка нажатия на кнопку "Отправить" - {@code sendMessageButton}</h3>
     */
    @FXML
    protected void onClickSendMessageButton()
    {
        SIGNAL = 2;
    }

    /**
     * <h3>Инициализация контроллера.</h3>
     *
     * <p>Добавляет прослушивателя к полю ввода номера порта. В подключаемом прослушивателе происходит волидация вводимой
     * строки.</p>
     *
     * <p><i>Nota bene</i>: метод инициализации имеет доступ к объектам интерфейса FXML, поэтому использование конструктора
     * класса невозможно в подобных ситуациях.</p>
     */
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

    /**
     * <h3>Проверка возможности соединения</h3>
     *
     * @return если соединение возможно или необходимо восстановить, то {@code True}, иначе {@code False}
     */
    private boolean canConnect()
    {
        if (connected)
            return false;

        if (portAutoNumberCheckBox.isSelected())
            return true;

        return getPortNumberTextFieldValue() != ERROR_PORT_NUMBER;
    }

    /**
     * <h3>Возвращает численное значения поля ввода номера порта</h3>
     * @return номер порта или {@code Server.ERROR_PORT_NUMBER}
     */
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

    /**
     * <h2>Класс для установки соединения в дочернем потоке</h2>
     */
    class ConnectionServer implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        portNumberTextField.setText(Integer.toString(server.getLocalPort()));
                    }
                });

                server.connection();

                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        connectionProgressBar.setVisible(false);

                        connectionEstablishedLabel.setVisible(true);

                        disconnectButton.setDisable(false);
                    }
                });

                Thread.sleep(100);

                server.connectionProtection();


                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        messageTextArea.setDisable(false);
                        sendMessageButton.setDisable(false);
                    }
                });

                connected  = true;

                Thread synchronization = new Thread(new Synchronization());

                synchronization.setDaemon(true);

                synchronization.start();
            }
            catch (Server.ConnectionException ex)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        errorConnectionLabel.setText(ex.getMessage());

                        errorConnectionLabel.setVisible(true);
                        connectionProgressBar.setVisible(false);

                        connectionButton.setDisable(!canConnect());
                        portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                        portAutoNumberCheckBox.setDisable(false);
                    }
                });

                server.disconnect();


                throw new RuntimeException();
            }
            catch (Server.ConnectionProtectionException ex)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        connectionNotSecureLabel.setText(ex.getMessage());

                        connectionNotSecureLabel.setVisible(true);

                        connectionButton.setDisable(!canConnect());
                        portNumberTextField.setDisable(portAutoNumberCheckBox.isSelected());
                        portAutoNumberCheckBox.setDisable(false);
                    }
                });

                server.disconnect();

                throw new RuntimeException();
            }
            catch (InterruptedException ex)
            {
                server.disconnect();

                System.out.println(ex.getMessage());
            }
        }
    }

    /**
     * <h2>Класс для работы прогресбара в дочернем потоке</h2>
     */
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

            while (progressValue > 0 && !connected)
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
                    Thread.sleep(98);
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

    /**
     * <h2>Класс для синхронизации работы серверного и клиентского приложений</h2>
     */
    class Synchronization implements Runnable
    {
        @Override
        public void run()
        {

            try
            {
                Thread.sleep(250);

                switch (SIGNAL)
                {
                    case WAITING ->
                    {
                        server.synchronizationOut(WAITING);
                    }
                    case SEND_MESSAGE ->
                    {
                        server.synchronizationOut(SEND_MESSAGE);

                        String message = messageTextArea.getText();

                        if (message.length() == 0)
                            return;

                        server.sendMessage(message);

                        messageTextArea.setText("");

                        SIGNAL = WAITING;
                    }
                    case DISCONNECT ->
                    {
                        disconnect();
                        return;
                    }
                }

                exit:
                while (true)
                {
                    Thread.sleep(500);

                    int signal = DISCONNECT;

                    if (connected)
                        signal = server.synchronizationIn();

                    switch (signal)
                    {
                        case WAITING ->
                        {
//                            System.out.println("Ждём");
                        }
                        case MESSAGE_DELIVERED ->
                        {
//                            signal = client.synchronization();
//                            System.out.println("Ооо кошелёк!!!");
                        }
                        case DISCONNECT ->
                        {
                            disconnect();

                            break exit;
                        }
                    }

                    switch (SIGNAL)
                    {
                        case WAITING ->
                        {
                            server.synchronizationOut(WAITING);
                        }
                        case SEND_MESSAGE ->
                        {
                            server.synchronizationOut(SEND_MESSAGE);

                            String message = messageTextArea.getText();

                            if (message.length() == 0)
                                return;

                            server.sendMessage(message);

                            messageTextArea.setText("");

                            SIGNAL = WAITING;
                        }
                        case DISCONNECT ->
                        {
                            disconnect();

                            break exit;
                        }
                    }
                }
            }
            catch (Server.SynchronizationException ex)
            {
                System.out.println(ex.getMessage());
                disconnect();
            }
            catch (Server.SendMessageException ex)
            {
                errorSendMessageLabel.setText(ex.getMessage());

                errorSendMessageLabel.setVisible(true);
            }
            catch (InterruptedException ex)
            {

            }
        }

        private void disconnect()
        {
            server.disconnect();

            connected = false;

            disconnectLabel.setVisible(true);
            connectionEstablishedLabel.setVisible(false);

            messageTextArea.setDisable(true);
            disconnectButton.setDisable(true);
            sendMessageButton.setDisable(true);
            portNumberTextField.setDisable(false);
            connectionButton.setDisable(!canConnect());
        }
    }
}
