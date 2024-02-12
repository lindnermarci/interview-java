package hu.bca.library.services;

import hu.bca.library.models.Book;

import java.util.Collection;

public interface BookService {
    Book addAuthor(Long bookId, Long authorId);

    Collection<Book> updateAllWithPublishYear();
}
