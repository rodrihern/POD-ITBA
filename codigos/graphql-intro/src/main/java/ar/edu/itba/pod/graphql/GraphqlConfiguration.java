package ar.edu.itba.pod.graphql;

import ar.edu.itba.pod.graphql.blog.dao.AuthorDao;
import ar.edu.itba.pod.graphql.blog.dao.PostDao;
import ar.edu.itba.pod.graphql.blog.model.Author;
import ar.edu.itba.pod.graphql.blog.model.Post;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphqlConfiguration {

    @Bean
    public PostDao postDao() {
        PostDao postDao = new PostDao();

        for (int postId = 0; postId < 10; ++postId) {
            for (int authorId = 0; authorId < 10; ++authorId) {
                Post post = new Post("Post" + authorId + postId, "Post " + authorId + ":" + postId, "Post category", "Post " + postId + " + by author " + authorId, "Author" + authorId);
                postDao.save(post);
            }
        }
        return postDao;
    }

    @Bean
    public AuthorDao authorDao() {
        AuthorDao authorDao = new AuthorDao();
        for (int authorId = 0; authorId < 10; ++authorId) {
            Author author = new Author(
                    "Author" + authorId,
                    "Author " + authorId);
            authorDao.save(author);
        }
        return authorDao;
    }
}