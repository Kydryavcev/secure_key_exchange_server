package model;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.*;

/**
 * <h2>Класс реализующий бизнес-логику сервера с шифрованием передаваемых данных</h2>
 *
 * <p>{@code Server} позволяет установить соединение с клиенским соединением при помощи метода
 * {@link Server#connection()}. Далее для корректной работы требуется защитить соединение. Это возможность реализуется
 * методом {@link Server#connectionProtection()}. Только после проведения этих операций, можно передавать сообщения
 * через метод {@link Server#sendMessage(String)}</p>
 *
 * @author Kydryavcev Ilya
 * @version 1.0
 * @since 12.03.24
 */
public class Server
{
    private ServerSocket serverSocket;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private Key secretKey;

    /**
     * <h2>Конструктор класса {@code Server}</h2>
     *
     * <p>Создает объект класса {@code ServerSocket} с указанным портом {@code port}. Задаёт время ожидания подключения
     * в 10 секунд.</p>
     *
     * @param port номер порта или 0, чтобы использовать номер порта, который назначается автоматически.
     *
     * @throws ServerInitializationException если при инициализации сервера произошла какая-то ошибка.
     */
    public Server(int port) throws ServerInitializationException
    {
        try
        {
            serverSocket = new ServerSocket(port);

            serverSocket.setSoTimeout(10000);
        }
        catch (SocketException ex)
        {
            throw new ServerInitializationException("Данный порт уже занят.");
        }
        catch (IllegalArgumentException ex)
        {
            throw new ServerInitializationException("Указан некорректный номер порта. Введите номер порта заново.");
        }
        catch (SecurityException ex)
        {
            throw new ServerInitializationException("Менеджер безопасности и его метод checkListen не разрешает операцию.");
        }
        catch (IOException ex)
        {
            throw new ServerInitializationException("При открытии сокета возникла ошибка ввода-вывода.");
        }
    }

    /**
     * <h3>Получение номера порта для установки соединения</h3>
     * @return номер порта
     */
    public int getLocalPort()
    {
        return serverSocket.getLocalPort();
    }

    /**
     * <h3>Установка сооединения с клиент-приложением</h3>
     *
     * @throws ConnectionException если по каким-то причинам не удалось установить соединение.
     */
    public void connection() throws ConnectionException
    {
        System.out.println("Ожидание клиента на порт " + serverSocket.getLocalPort() + "...");

        try
        {
            socket = serverSocket.accept();
        }
        catch (SocketTimeoutException ex)
        {
            throw new ConnectionException("Превышено время ожидания.");
        }
        catch (IOException ex)
        {
            throw new ConnectionException("Во время ожидания соединения возникла ошибка ввода-вывода.");
        }

        System.out.println("Подключение прошло успешно!");

        try
        {
            out = new DataOutputStream(socket.getOutputStream());
            in  = new DataInputStream(socket.getInputStream());
        }
        catch (IOException ex)
        {
            throw new ConnectionException("Во время ожидания соединения возникла ошибка ввода-вывода.");
        }
    }

    /**
     * <h3>Установка соединения в защищенное состояние</h3>
     *
     * <p>Сервер-объект генерирует пару ключей RSA. Открытый ключ передаёт по незащищенному каналу клиент-приложению.
     * С помощью открытого ключа клиент-приложение зашифровывает секретный симметричный ключ и отправляет полученный
     * шифр сервер-приложению. Затем сервер расшифровывает секретный ключ при помощи закрытого ключа. После получения
     * секретного ключа, передаваемые данные будут шифроваться и передоваться клиенту методом
     * {@link Server#sendMessage(String)}.</p>
     *
     * @throws ConnectionProtectionException если по каким-то причинам не получилось защитить канал передачи данных
     */
    public void connectionProtection()
            throws ConnectionProtectionException
    {
        if (socket == null)
            throw new ConnectionProtectionException("Нет связи с клиентским приложением.");

        KeyPair keyPair;

        try
        {
            keyPair = CryptographicAlgorithms.generateKeyPair();
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new ConnectionProtectionException("Провайдер не поддерживает реализация криптоалгоритма.");
        }

        PublicKey publicKey   = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        try
        {
            out.write(publicKey.getEncoded());

            byte[] wrappedKeyBytes = new byte[100], buffer = new byte[100];

            int lengthMessage = in.read(wrappedKeyBytes);

            while (in.available() > 0)
            {
                lengthMessage += in.read(buffer);

                byte[] temp = new byte[lengthMessage];

                System.arraycopy(wrappedKeyBytes, 0, temp,0, wrappedKeyBytes.length);
                System.arraycopy(buffer,0,temp,wrappedKeyBytes.length, lengthMessage - wrappedKeyBytes.length);

                wrappedKeyBytes = temp;
            }

            secretKey = CryptographicAlgorithms.unwrapKey(wrappedKeyBytes, privateKey);
        }
        catch (IOException ex)
        {
            throw new ConnectionProtectionException("Возникла ошибка ввода-вывода.");
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new ConnectionProtectionException("Провайдер не поддерживает реализация криптоалгоритма.");
        }
        catch (NoSuchPaddingException ex)
        {
            throw new ConnectionProtectionException("Криптографический механизм загружен и не доступен.");
        }
        catch (InvalidKeyException ex)
        {
            throw new ConnectionProtectionException("Установленный ключ не поддерживается для данного алгоритма.");
        }
    }

    /**
     * <h2>Отправка сообщения по защищенном каналу</h2>
     *
     * <p>Преобразует сообщение в массив байтов, шифрует его и отправляет клиент-приложению.</p>
     *
     * @param message сообщение для отправки.
     * @throws SendMessageException если канал не защищен или, если при шифровании возникли ошибки.
     */
    public void sendMessage(String message) throws SendMessageException
    {
        try
        {
            if (secretKey == null)
                throw new IOException("Соединение не защищено.");

            byte[] messageBytes = message.getBytes();

            byte[] cipherBytes = CryptographicAlgorithms.encrypt(messageBytes, secretKey);

            System.out.println(cipherBytes.length);

            out.write(cipherBytes);
        }
        catch (IOException ex)
        {
            throw new SendMessageException("Возникла ошибка ввода-вывода.");
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new SendMessageException("Провайдер не поддерживает реализация криптоалгоритма.");
        }
        catch (NoSuchPaddingException ex)
        {
            throw new SendMessageException("Криптографический механизм загружен и не доступен.");
        }
        catch (InvalidKeyException ex)
        {
            throw new SendMessageException("Установленный ключ не поддерживается для данного алгоритма.");
        }
        catch (IllegalBlockSizeException ex)
        {
            throw new SendMessageException("Длина сообщения не соответствует длине блока.");
        }
        catch (BadPaddingException ex)
        {
            throw new SendMessageException("Сообщение дополнено неверным образом.");
        }
    }

    /**
     * <h2>Проверка соединения</h2>
     * @return
     */
    public boolean isConnect()
    {
        return socket != null;
    }

    public boolean connected()
    {
        return true;
    }

    /**
     * <h2>Потерять соединение</h2>
     * @throws IOException если произошла ошибка ввода-вывода
     */
    public void disconnect() throws IOException
    {
        in.close();
        out.close();
        socket.close();
        serverSocket.close();
    }

    public class ServerInitializationException extends Exception
    {
        public ServerInitializationException(String message)
        {
            super(message);
        }
    }

    public class ConnectionException extends Exception
    {
        public ConnectionException(String message)
        {
            super(message);
        }
    }

    public class ConnectionProtectionException extends Exception
    {
        public ConnectionProtectionException(String message)
        {
            super(message);
        }
    }

    public class SendMessageException extends Exception
    {
        public SendMessageException(String message)
        {
            super(message);
        }
    }
}

