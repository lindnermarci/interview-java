package hu.bca.library.models;


import hu.bca.library.feign.model.FirstPublishDateWrapper;

public record BookWithPublishDate(Book book, FirstPublishDateWrapper firstPublishDateWrapper) {}
