package ar.edu.itba.pod.graphql.blog.dao;

import ar.edu.itba.pod.graphql.blog.model.Author;
import ar.edu.itba.pod.graphql.dao.BaseDao;

public class AuthorDao extends BaseDao<Author> {
    public AuthorDao() {
        super(Author::id);
    }

    public Author getAuthor(final String authorId) {
        return findById(authorId).orElseThrow(() -> new IllegalArgumentException("no author with id " + authorId));
    }
}
