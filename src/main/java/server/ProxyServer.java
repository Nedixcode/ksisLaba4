package server;

import blacklist.Blacklist;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer {

    private final int port;
    private final Blacklist blacklist;

    public ProxyServer(int port, String blacklistPath) {
        this.port = port;
        this.blacklist = new Blacklist(blacklistPath);
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("HTTP-прокси запущен на порту " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, blacklist);
                Thread t = new Thread(handler);
                t.start();
            }
        }
    }
}
