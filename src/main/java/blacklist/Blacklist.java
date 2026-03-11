package blacklist;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Blacklist {

    private final List<String> entries = new ArrayList<>();

    public Blacklist(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("Файл чёрного списка не найден: " + path);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    entries.add(line.toLowerCase());
                }
            }
            System.out.println("Загружено " + entries.size() + " записей чёрного списка.");
        } catch (IOException e) {
            System.err.println("Ошибка чтения чёрного списка: " + e.getMessage());
        }
    }

    public boolean isBlocked(String url, String host) {
        String urlLower = url.toLowerCase();
        String hostLower = host.toLowerCase();

        for (String entry : entries) {
            if (entry.startsWith("http://") || entry.startsWith("https://")) {
                if (urlLower.startsWith(entry)) {
                    return true;
                }
            } else {
                if (hostLower.equals(entry) || hostLower.endsWith("." + entry)) {
                    return true;
                }
            }
        }
        return false;
    }
}
