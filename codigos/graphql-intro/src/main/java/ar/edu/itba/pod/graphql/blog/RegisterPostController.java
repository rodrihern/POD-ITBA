package ar.edu.itba.pod.graphql.blog;

import ar.edu.itba.pod.graphql.blog.dao.AuthorDao;
import ar.edu.itba.pod.graphql.blog.dao.PostDao;
import ar.edu.itba.pod.graphql.blog.model.Author;
import ar.edu.itba.pod.graphql.blog.model.Post;
import org.dataloader.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

//@Controller
public class RegisterPostController {
    private static final Logger logger = LoggerFactory.getLogger(RegisterPostController.class);

    private final PostDao postDao;
    private final AuthorDao authorDao;

    public RegisterPostController(final PostDao postDao, final AuthorDao authorDao, BatchLoaderRegistry registry) {
        this.postDao = postDao;
        this.authorDao = authorDao;
        registry.forTypePair(String.class, Author.class).registerMappedBatchLoader((authorIds, env) -> {
            logger.info("RRRlooking for author for {} posts", authorIds.size());

            Map<String, Author> authors = new HashMap<>();

            authorIds.forEach(id -> authors.put(id, authorDao.getAuthor(id)));
            return Mono.just(authors);
        });
    }

    @QueryMapping
    public List<Post> recentPosts(@Argument int count, @Argument int offset) {
        return postDao.getRecentPosts(count, offset);
    }

    @MutationMapping
    public Post createPost(@Argument String title, @Argument String text,
                           @Argument String category, @Argument String authorId) {

        Post post = new Post(
                UUID.randomUUID().toString(),
                title,
                text,
                category,
                authorId);

        postDao.savePost(post);

        return post;
    }


    @SchemaMapping
    public CompletableFuture<Author> author(Post post, DataLoader<String, Author> loader) {
        return loader.load(post.authorId());
    }
}
