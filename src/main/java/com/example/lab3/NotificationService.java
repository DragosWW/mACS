package com.example.lab3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final String endpoint;

    public NotificationService(@Value("${ntfy.url}") String url,
                               @Value("${ntfy.topic}") String topic) {
        this.endpoint = url + "/" + topic;
    }

    /** Rulează pe alt thread, deci nu încetinește răspunsul la crearea postării. */
    @Async
    public void postCreated(Post post) {
        String message = """
                New post created
                User: %s
                Post ID: %d
                Title: %s
                Time: %s
                """.formatted(post.getUsername(), post.getId(), post.getTitle(), post.getCreatedAt());

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Title", "Instakill - new post")
                .header("Tags", "memo")
                .POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("ntfy notification for post {} -> HTTP {}", post.getId(), res.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("ntfy notification failed for post {}: {}", post.getId(), e.getMessage());
        }
    }
}