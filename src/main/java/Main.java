import server.ProxyServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        int port = 8888;

        String blacklistPath = "src/main/java/blacklist/Blacklist.txt";

        ProxyServer proxyServer = new ProxyServer(port, blacklistPath);
        try {
            proxyServer.start();
        } catch (IOException e) {
            System.err.println("Ошибка при запуске прокси-сервера: " + e.getMessage());
        }
    }
}
