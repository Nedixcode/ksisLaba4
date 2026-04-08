package server;

import blacklist.Blacklist;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer {

    private final int port;
    private final Blacklist blacklist;
    private final InetAddress bindAddress;

    public ProxyServer(int port, String blacklistPath, String bindIp) throws IOException {
        this.port = port;
        this.blacklist = new Blacklist(blacklistPath);
        this.bindAddress = InetAddress.getByName(bindIp);
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port, 50, bindAddress)) {
            System.out.println("HTTP-прокси запущен на " +
                    bindAddress.getHostAddress() + ":" + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, blacklist);
                Thread t = new Thread(handler);
                t.start();
            }
        }
    }
}