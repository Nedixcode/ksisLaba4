package http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HttpRequest {

    private final String method;
    private final String fullUrl;
    private final String path;
    private final String httpVersion;
    private final List<String> headers;
    private final String host;
    private final int port;
    private final int contentLength;

    public HttpRequest(String method,
                       String fullUrl,
                       String path,
                       String httpVersion,
                       List<String> headers,
                       String host,
                       int port,
                       int contentLength) {
        this.method = method;
        this.fullUrl = fullUrl;
        this.path = path;
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.host = host;
        this.port = port;
        this.contentLength = contentLength;
    }

    public static HttpRequest parse(String requestLine, List<String> headers) {
        try {
            String[] parts = requestLine.split(" ");
            if (parts.length < 3) {
                return null;
            }

            String method = parts[0];
            String uri = parts[1];
            String httpVersion = parts[2];

            String host = null;
            int port = 80;
            int contentLength = 0;

            List<String> newHeaders = new ArrayList<>();

            for (String header : headers) {
                String lower = header.toLowerCase();
                if (lower.startsWith("host:")) {
                    String value = header.substring(5).trim();
                    host = value;
                    if (value.contains(":")) {
                        String[] hp = value.split(":", 2);
                        host = hp[0].trim();
                        try {
                            port = Integer.parseInt(hp[1].trim());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                } else if (lower.startsWith("content-length:")) {
                    String value = header.substring("content-length:".length()).trim();
                    try {
                        contentLength = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                    }
                }
                newHeaders.add(header);
            }

            String fullUrl;
            String path;

            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                URL url = new URL(uri);
                fullUrl = uri;
                path = url.getFile().isEmpty() ? "/" : url.getFile();
                if (host == null) {
                    host = url.getHost();
                }
                if (port == 80 && url.getPort() != -1) {
                    port = url.getPort();
                }
                boolean hasHostHeader = false;
                for (String h : newHeaders) {
                    if (h.toLowerCase().startsWith("host:")) {
                        hasHostHeader = true;
                        break;
                    }
                }
                if (!hasHostHeader) {
                    String hostHeader = "Host: " + host + (url.getPort() != -1 ? (":" + url.getPort()) : "");
                    newHeaders.add(hostHeader);
                }
            } else {
                if (host == null) {
                    return null;
                }
                fullUrl = "http://" + host + uri;
                path = uri;
            }

            if (host == null) {
                return null;
            }

            return new HttpRequest(method, fullUrl, path, httpVersion, newHeaders, host, port, contentLength);

        } catch (MalformedURLException e) {
            return null;
        }
    }

    public String getMethod() {
        return method;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public String getPath() {
        return path;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getContentLength() {
        return contentLength;
    }
}
