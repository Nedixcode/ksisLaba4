package server;

import blacklist.Blacklist;
import http.HttpRequest;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {

    private static final int SERVER_SOCKET_TIMEOUT_MS = 30_000;
    private static final int BUFFER_SIZE = 8 * 1024;

    private final Socket clientSocket;
    private final Blacklist blacklist;

    public ClientHandler(Socket clientSocket, Blacklist blacklist) {
        this.clientSocket = clientSocket;
        this.blacklist = blacklist;
    }

    @Override
    public void run() {
        try (
                InputStream clientIn = clientSocket.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientIn))
        ) {
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            List<String> headers = readHeaders(reader);
            HttpRequest httpRequest = HttpRequest.parse(requestLine, headers);
            if (httpRequest == null) {
                return;
            }

            String fullUrl = httpRequest.getFullUrl();
            String host = httpRequest.getHost();
            int port = httpRequest.getPort();

            if (blacklist.isBlocked(fullUrl, host)) {
                sendBlockedResponse(clientOut, fullUrl);
                log(fullUrl, 403);
                return;
            }

            try (Socket serverSocket = new Socket(host, port)) {
                serverSocket.setSoTimeout(SERVER_SOCKET_TIMEOUT_MS);

                OutputStream serverOut = serverSocket.getOutputStream();
                InputStream serverIn = serverSocket.getInputStream();

                sendRequestToServer(httpRequest, serverOut, reader);
                forwardResponse(serverIn, clientOut, fullUrl);

            } catch (IOException e) {
            }

        } catch (IOException e) {
            System.err.println("Ошибка при обработке клиента: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private List<String> readHeaders(BufferedReader reader) throws IOException {
        List<String> headers = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            headers.add(line);
        }
        return headers;
    }

    private void sendRequestToServer(HttpRequest request,
                                     OutputStream serverOut,
                                     BufferedReader clientReader) throws IOException {
        PrintWriter serverWriter = new PrintWriter(new OutputStreamWriter(serverOut), false);

        serverWriter.print(request.getMethod() + " " + request.getPath() + " " + request.getHttpVersion() + "\r\n");
        for (String header : request.getHeaders()) {
            serverWriter.print(header + "\r\n");
        }
        serverWriter.print("\r\n");
        serverWriter.flush();

        int contentLength = request.getContentLength();
        if (contentLength > 0) {
            char[] body = new char[contentLength];
            int read = 0;
            while (read < contentLength) {
                int r = clientReader.read(body, read, contentLength - read);
                if (r == -1) {
                    break;
                }
                read += r;
            }
            serverWriter.print(body);
            serverWriter.flush();
        }
    }

    private void forwardResponse(InputStream serverIn, OutputStream clientOut, String url) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(serverIn);
        BufferedOutputStream bos = new BufferedOutputStream(clientOut);

        ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
        int prev = -1;
        int cur;
        boolean headersEnded = false;

        while ((cur = bis.read()) != -1) {
            headerBuffer.write(cur);
            if (prev == '\r' && cur == '\n') {
                byte[] buf = headerBuffer.toByteArray();
                int len = buf.length;
                if (len >= 4 &&
                        buf[len - 4] == '\r' &&
                        buf[len - 3] == '\n' &&
                        buf[len - 2] == '\r' &&
                        buf[len - 1] == '\n') {
                    headersEnded = true;
                    break;
                }
            }
            prev = cur;
        }

        if (!headersEnded) {
            return;
        }

        byte[] headerBytes = headerBuffer.toByteArray();
        String headerText = new String(headerBytes, StandardCharsets.ISO_8859_1);
        String[] headerLines = headerText.split("\r\n");

        int statusCode = parseStatusCode(headerLines);
        log(url, statusCode);

        bos.write(headerBytes);
        bos.flush();

        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = bis.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
            bos.flush();
        }
    }

    private int parseStatusCode(String[] headerLines) {
        if (headerLines.length == 0) {
            return 0;
        }
        String statusLine = headerLines[0];
        String[] parts = statusLine.split(" ");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private void sendBlockedResponse(OutputStream clientOut, String url) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientOut, StandardCharsets.UTF_8), false);
        String body = "<html><body><h2>Доступ к ресурсу заблокирован</h2>" +
                "<p>URL: " + url + "</p>" +
                "</body></html>";

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        writer.print("HTTP/1.1 403 Forbidden\r\n");
        writer.print("Content-Type: text/html; charset=UTF-8\r\n");
        writer.print("Content-Length: " + bodyBytes.length + "\r\n");
        writer.print("Connection: close\r\n");
        writer.print("\r\n");
        writer.flush();

        clientOut.write(bodyBytes);
        clientOut.flush();
    }

    private void log(String url, int statusCode) {
        System.out.println("URL: " + url + " | Response: " + statusCode);
    }
}