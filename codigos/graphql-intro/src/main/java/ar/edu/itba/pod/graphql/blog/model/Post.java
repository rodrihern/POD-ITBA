package ar.edu.itba.pod.graphql.blog.model;

public record Post(
        String id,
        String title,
        String text,
        String category,
        String authorId
) {
}