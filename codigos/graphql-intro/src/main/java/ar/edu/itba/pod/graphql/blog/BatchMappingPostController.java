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

//@Controller
public class BatchMappingPostController {
    private static final Logger logger = LoggerFactory.getLogger(BatchMappingPostController.class);

    private final PostDao postDao;
    private final AuthorDao authorDao;

    public BatchMappingPostController(final PostDao postDao, final AuthorDao authorDao) {
        this.postDao = postDao;
        this.authorDao = authorDao;
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


    @BatchMapping
    public Mono<Map<Post, Author>> author(List<Post> posts) {
        logger.info("looking for author for {} posts", posts.size());
        Map<Post, Author> authors = new HashMap<>();

        posts.forEach(p -> authors.put(p, authorDao.getAuthor(p.authorId())));
        return Mono.just(authors);
    }

}
