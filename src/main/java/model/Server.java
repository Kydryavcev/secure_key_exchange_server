package model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class Server
{
    private ServerSocket serverSocket;
    private Socket socket;
    private DataOutputStream out;

    /**
     * <h2>Конструктор класса {@code Server}</h2>
     *
     * <p>Создает объект класса {@code ServerSocket} с указанным портом {@code port}. Задаёт время ожидания подключения
     * в 10 секунд.</p>
     *
     * @param port номер порта или 0, чтобы использовать номер порта, который назначается автоматически.
     *
     * @throws IOException если при открытии сокета возникла ошибка ввода-вывода.
     * @throws SecurityException если существует менеджер безопасности и его метод checkListen не разрешает операцию.
     * @throws IllegalArgumentException если параметр порта находится за пределами указанного диапазона допустимых
     * значений порта, который находится в диапазоне от 0 до 65535 включительно.
     * @throws SocketException если в базовом протоколе возникла ошибка, например ошибка TCP.
     */
    public void Server(int port) throws IOException, SecurityException, IllegalArgumentException, SocketException
    {
        serverSocket = new ServerSocket(port);

        serverSocket.setSoTimeout(10000);
    }

    public void connection() throws IOException, SocketTimeoutException
    {
        System.out.println("Ожидание клиента на порт " + serverSocket.getLocalPort() + "...");

        socket = serverSocket.accept();

        System.out.println("Подключение прошло успешно!");

        out = new DataOutputStream(socket.getOutputStream());
    }

    public void disconnect() throws IOException
    {
        out.close();
        socket.close();
        serverSocket.close();
    }
}
