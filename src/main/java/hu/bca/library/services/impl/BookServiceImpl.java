package hu.bca.library.services.impl;

import hu.bca.library.feign.OpenLibraryClient;
import hu.bca.library.feign.OpenLibraryClientAsync;
import hu.bca.library.models.Author;
import hu.bca.library.models.Book;
import hu.bca.library.models.BookWithPublishDate;
import hu.bca.library.repositories.AuthorRepository;
import hu.bca.library.repositories.BookRepository;
import hu.bca.library.services.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class BookServiceImpl implements BookService {

    private final Pattern pattern = Pattern.compile("\\b\\d{4}\\b");

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final OpenLibraryClient openLibraryClient;
    private final OpenLibraryClientAsync openLibraryClientAsync;

    public BookServiceImpl(BookRepository bookRepository, AuthorRepository authorRepository, OpenLibraryClient openLibraryClient, OpenLibraryClientAsync openLibraryClientAsync) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.openLibraryClient = openLibraryClient;
        this.openLibraryClientAsync = openLibraryClientAsync;
    }

    @Override
    public Book addAuthor(Long bookId, Long authorId) {
        Optional<Book> book = this.bookRepository.findById(bookId);
        if (book.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Book with id %s not found", bookId));
        }
        Optional<Author> author = this.authorRepository.findById(authorId);
        if (author.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Author with id %s not found", authorId));
        }

        List<Author> authors = book.get().getAuthors();
        authors.add(author.get());

        book.get().setAuthors(authors);
        return this.bookRepository.save(book.get());
    }


    @Override
    public Collection<Book> updateAllWithYear() {
        Iterable<Book> books = bookRepository.findAll();
        List<Book> booksToSave = StreamSupport.stream(books.spliterator(), false)
          .parallel()
          .map(openLibraryClientAsync::getBookWithFirstPublishDateAsync)
          .map(CompletableFuture::join)
          .map(this::getBookWithPublishedYear)
          .collect(Collectors.toList());

        Iterable<Book> savedBooks = bookRepository.saveAll(booksToSave);
        return StreamSupport.stream(savedBooks.spliterator(), false)
          .toList();
    }

    private Book getBookWithPublishedYear(final BookWithPublishDate bookWithPublishDate) {
        if (!StringUtils.hasLength(bookWithPublishDate.firstPublishDateWrapper().getFirstPublishDate())) {
            return bookWithPublishDate.book();
        }
        Matcher matcher = pattern.matcher(bookWithPublishDate.firstPublishDateWrapper().getFirstPublishDate());
        if (matcher.find()) {
            var year = matcher.group();
            bookWithPublishDate.book().setYear(Integer.valueOf(year));
        }
        return bookWithPublishDate.book();
    }
}
