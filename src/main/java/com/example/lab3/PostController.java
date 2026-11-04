package com.example.lab3;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostRepository repository;
    private final NotificationService notifications;

    public PostController(PostRepository repository, NotificationService notifications) {
        this.repository = repository;
        this.notifications = notifications;
    }

    @GetMapping
    public List<Post> all() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Post create(@RequestBody Post post, Authentication auth) {
        post.setId(null);
        post.setUsername(auth.getName());
        post.setCreatedAt(Instant.now());
        Post saved = repository.save(post);
        notifications.postCreated(saved); // asincron
        return saved;
    }
}