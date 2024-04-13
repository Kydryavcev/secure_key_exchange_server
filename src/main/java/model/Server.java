package model;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
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

    private Signature signature;

    /**
     * <h3>Инициализация класса создания ЭЦП.</h3>
     *
     * @param alias псевдоним записи.
     * @param password пароль от хранилища ключей.
     *
     * @throws UnrecoverableKeyException если задан не верный пароль {@code password}.
     * @throws NullPointerException если запись с псевдонимом {@code alias} не существует.
     */
    public void initSignature(String alias, String password)
            throws UnrecoverableKeyException, NullPointerException
    {
        try
        {
            PrivateKey privateKey = CryptographicAlgorithms.getPrivateKey(alias, password);

            signature = Signature.getInstance("MD5withRSA");

            signature.initSign(privateKey);
        }
        catch (NoSuchAlgorithmException|java.security.InvalidKeyException ex)
        {
            try (FileWriter fw = new FileWriter("src/main/resources/logs/.log"))
            {
                fw.write(java.time.LocalDateTime.now().toString());
                fw.write(CryptographicAlgorithms.class.getName() + "\n");
                fw.write(new Exception().getStackTrace()[0].getMethodName() + "\n");
                fw.write(ex.getClass().getName() + "\n");
                fw.write(ex.getMessage() + "\n");
            }
            catch (IOException ex1)
            {
                System.out.println("Файла для логирования не существует");
                System.out.println(ex1.getMessage());
            }
        }
    }

    /**
     * <h2>Выделение сокета</h2>
     *
     * <p>Создает объект класса {@code ServerSocket} с указанным портом {@code port}. Задаёт время ожидания подключения
     * в 10 секунд.</p>
     *
     * @param port номер порта или 0, чтобы использовать номер порта, который назначается автоматически.
     *
     * @throws HighlightSocketException если при инициализации сервера произошла какая-то ошибка.
     */
    public void highlightSocket(int port) throws HighlightSocketException
    {
        try
        {
            serverSocket = new ServerSocket(port);

            serverSocket.setSoTimeout(10000);
        }
        catch (SocketException ex)
        {
            throw new HighlightSocketException("Данный порт уже занят.");
        }
        catch (IllegalArgumentException ex)
        {
            throw new HighlightSocketException("Указан некорректный номер порта. Введите номер порта заново.");
        }
        catch (SecurityException ex)
        {
            throw new HighlightSocketException("Менеджер безопасности и его метод checkListen не разрешает операцию.");
        }
        catch (IOException ex)
        {
            throw new HighlightSocketException("При открытии сокета возникла ошибка ввода-вывода.");
        }
    }

    public Socket getSocket()
    {
        return socket;
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
//        System.out.println("Ожидание клиента на порт " + serverSocket.getLocalPort() + "...");

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
    public void connectionProtection() throws ConnectionProtectionException
    {
        if (socket == null)
            throw new ConnectionProtectionException("Нет связи с клиентским приложением.");

        KeyPair keyPair;

        keyPair = CryptographicAlgorithms.generateKeyPair();

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

            if (secretKey == null)
                throw new NullPointerException("В ходе развёртки секретного ключа произошла ошибка.");
        }
        catch (IOException ex)
        {
            throw new ConnectionProtectionException("Возникла ошибка ввода-вывода.");
        }
        catch (NullPointerException ex)
        {
            throw new ConnectionProtectionException(ex.getMessage());
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

//            System.out.println("Длина в байтах: " + messageBytes.length);

            byte[] lengthMessageBytes = intToBs(messageBytes.length);

            signature.update(messageBytes);

            byte[] sign = signature.sign();

//            System.out.println("Length sing: " + sign.length);

            byte[] finalMessageBytes = new byte[lengthMessageBytes.length + messageBytes.length + sign.length];

            System.arraycopy(lengthMessageBytes, 0, finalMessageBytes, 0, lengthMessageBytes.length);
            System.arraycopy(messageBytes, 0, finalMessageBytes, lengthMessageBytes.length, messageBytes.length);
            System.arraycopy(sign, 0, finalMessageBytes, lengthMessageBytes.length + messageBytes.length, sign.length);
//            System.out.println("Length finalMessageBytes: " + finalMessageBytes.length);
            byte[] cipherBytes = CryptographicAlgorithms.encrypt(finalMessageBytes, secretKey);

            if (cipherBytes == null)
                throw new NullPointerException("В ходе шифрования сообщения произошла ошибка.");

            out.write(cipherBytes);
        }
        catch (IOException ex)
        {
            throw new SendMessageException("Возникла ошибка ввода-вывода.");
        }
        catch (java.security.SignatureException ex)
        {
            throw new SendMessageException("Класс формирования ЭЦП не инициализирован.");
        }
        catch (NullPointerException ex)
        {
            throw new SendMessageException(ex.getMessage());
        }

    }


    public int synchronizationIn() throws SynchronizationException
    {
        int result = - 1;

        try
        {
            if (in.available() > 0)
            {
                result = in.read();
            }
        }
        catch (IOException ex)
        {
            throw new SynchronizationException("Возникла ошибка ввода-вывода.");
        }

        return result;
    }

    public void synchronizationOut(int signal) throws SynchronizationException
    {
        try
        {
            out.write(signal);
        }
        catch (IOException ex)
        {
            throw new SynchronizationException("Возникла ошибка ввода-вывода.");
        }
    }

    /**
     * <h2>Потерять соединение</h2>
     */
    public void disconnect()
    {
        try
        {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            serverSocket.close();
        }
        catch (IOException ex)
        {
            try (FileWriter fw = new FileWriter("src/main/resources/logs/.log"))
            {
                fw.write(java.time.LocalDateTime.now().toString());
                fw.write(CryptographicAlgorithms.class.getName() + "\n");
                fw.write(new Exception().getStackTrace()[0].getMethodName() + "\n");
                fw.write(ex.getClass().getName() + "\n");
                fw.write(ex.getMessage() + "\n");
            }
            catch (IOException ex1)
            {
                System.out.println("Файла для логирования не существует");
                System.out.println(ex1.getMessage());
            }
        }
    }

    public class HighlightSocketException extends Exception
    {
        public HighlightSocketException(String message)
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

    public class SynchronizationException extends Exception
    {
        public SynchronizationException(String message)
        {
            super(message);
        }
    }

    /**
    * <h2>Преобразование числа типа данных int в массив байтов.</h2>
    */
    private byte[] intToBs(int num)
    {
        byte[] bytes = new byte[4];

        for (int i = 0; i < 4; i++)
        {
            byte b = (byte)(num & 0xFF);

            num >>= 4;

            bytes[i] = b;
        }

        return bytes;
    }
}

