package ar.edu.itba.pod.graphql.blog.dao;

import ar.edu.itba.pod.graphql.blog.model.Post;
import ar.edu.itba.pod.graphql.dao.BaseDao;

import java.util.List;
import java.util.stream.Collectors;


public class PostDao extends BaseDao<Post> {
    public PostDao() {
        super(Post::id);
    }

    public List<Post> getRecentPosts(final int count, final int offset) {
        return getAll().stream().skip(offset).limit(count).toList();
    }

    public List<Post> getAuthorPosts(String author) {
        return getAll().stream()
                .filter(post -> author.equals(post.authorId()))
                .collect(Collectors.toList());
    }

    public void savePost(final Post post) {
        save(post);
    }
}
