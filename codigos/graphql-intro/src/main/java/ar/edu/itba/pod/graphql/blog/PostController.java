package ar.edu.itba.pod.graphql.blog;

import ar.edu.itba.pod.graphql.blog.dao.AuthorDao;
import ar.edu.itba.pod.graphql.blog.dao.PostDao;
import ar.edu.itba.pod.graphql.blog.model.Author;
import ar.edu.itba.pod.graphql.blog.model.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class PostController {
    private final PostDao postDao;
    private final AuthorDao authorDao;
    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    public PostController(PostDao postDao, AuthorDao authorDao) {
        this.postDao = postDao;
        this.authorDao = authorDao;
    }

    @QueryMapping 
    public List<Post> recentPosts(@Argument int count, @Argument int offset) {
        return postDao.getRecentPosts(count, offset);
    }

    // El del ejercicio 3
    // @SchemaMapping(typeName = "Post", field = "author") 
    // public Author getAuthor(Post post) {
    //     logger.info("looking for author of post {}", post.id());
    //     return authorDao.getAuthor(post.authorId());
    // }

    // El del ejercicio 4
    @BatchMapping public Mono<Map<Post, Author>> author(List<Post> posts) {
        logger.info("looking for author for {} posts", posts.size());
        Map<Post, Author> authors = new HashMap<>();
        for (Post post : posts) {
            authors.put(post, authorDao.getAuthor(post.authorId()));
        }
        return Mono.just(authors); 
    }

    @MutationMapping public Post createPost(@Argument String title, @Argument String text, @Argument String category, @Argument String authorId) {
        Post post = new Post( UUID.randomUUID().toString(), title, text, category, authorId);
        postDao.savePost(post); 
        return post; 
    }
}
