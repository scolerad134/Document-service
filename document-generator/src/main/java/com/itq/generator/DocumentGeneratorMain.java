package com.itq.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

/**
 * Утилита генерации документов.
 * Читает из generator.properties параметр count=N и создаёт N документов через API сервиса.
 */
public class DocumentGeneratorMain {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_CONFIG = "generator.properties";
    private static final String BASE_URL_KEY = "api.baseUrl";
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api";
    private static final String COUNT_KEY = "count";

    public static void main(String[] args) throws Exception {
        Properties props = loadProperties(args);
        String baseUrl = props.getProperty(BASE_URL_KEY, DEFAULT_BASE_URL).replaceAll("/$", "");
        int count = Integer.parseInt(props.getProperty(COUNT_KEY, "0"));

        if (count <= 0) {
            System.err.println("Parameter 'count' must be positive. Set in generator.properties or -Dcount=N");
            System.exit(1);
        }

        System.out.println("Document Generator: creating " + count + " documents via " + baseUrl);
        long totalStart = System.currentTimeMillis();

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        int success = 0;
        int failed = 0;

        for (int i = 0; i < count; i++) {
            long start = System.currentTimeMillis();
            boolean ok = createDocument(client, baseUrl, "Generator", "Generator", "Document #" + (i + 1));
            long elapsed = System.currentTimeMillis() - start;

            if (ok) {
                success++;
                if ((i + 1) % 100 == 0 || i == count - 1) {
                    System.out.println("Progress: " + (i + 1) + "/" + count + " created");
                }
            } else {
                failed++;
                System.err.println("Failed to create document " + (i + 1));
            }
        }

        long totalElapsed = System.currentTimeMillis() - totalStart;
        System.out.println("Done. Total: " + count + ", success=" + success + ", failed=" + failed);
        System.out.println("Total time: " + totalElapsed + " ms");
    }

    private static Properties loadProperties(String[] args) throws IOException {
        Properties props = new Properties();

        Path externalConfig = args.length > 0 ? Path.of(args[0]) : Path.of(DEFAULT_CONFIG);
        if (Files.exists(externalConfig)) {
            try (InputStream in = Files.newInputStream(externalConfig)) {
                props.load(in);
            }
        } else {
            try (InputStream in = DocumentGeneratorMain.class.getResourceAsStream("/" + DEFAULT_CONFIG)) {
                if (in != null) props.load(in);
            }
        }

        String countOverride = System.getProperty(COUNT_KEY);
        if (countOverride != null) {
            props.setProperty(COUNT_KEY, countOverride);
        }
        return props;
    }

    private static boolean createDocument(HttpClient client, String baseUrl, String initiator, String author, String title) {
        ObjectNode body = MAPPER.createObjectNode()
            .put("initiator", initiator)
            .put("author", author)
            .put("title", title);
        String json;
        try {
            json = MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            return false;
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/documents"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .timeout(Duration.ofSeconds(30))
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            System.err.println("Request failed: " + e.getMessage());
            return false;
        }
    }
}
