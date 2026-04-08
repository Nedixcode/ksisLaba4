import server.ProxyServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        int port = 8888;
        String bindIp = "127.0.0.5";
        String blacklistPath = "src/main/java/blacklist/Blacklist.txt";

        try {
            ProxyServer proxyServer = new ProxyServer(port, blacklistPath, bindIp);
            proxyServer.start();
        } catch (IOException e) {
            System.err.println("Ошибка при запуске прокси-сервера: " + e.getMessage());
        }
    }
}