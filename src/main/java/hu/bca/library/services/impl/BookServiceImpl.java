package hu.bca.library.services.impl;

import hu.bca.library.feign.OpenLibraryClient;
import hu.bca.library.feign.OpenLibraryClientAsync;
import hu.bca.library.models.Author;
import hu.bca.library.models.Book;
import hu.bca.library.models.BookWithPublishDate;
import hu.bca.library.repositories.AuthorRepository;
import hu.bca.library.repositories.BookRepository;
import hu.bca.library.services.BookService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Service
@Log4j2
public class BookServiceImpl implements BookService {

    private final Pattern pattern = Pattern.compile("\\b\\d{4}\\b");

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final OpenLibraryClientAsync openLibraryClientAsync;

    public BookServiceImpl(BookRepository bookRepository, AuthorRepository authorRepository, OpenLibraryClientAsync openLibraryClientAsync) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.openLibraryClientAsync = openLibraryClientAsync;
    }

    @Override
    public List<Book> getAllBooksFromCountryPublishedLaterThan(final String countryCode, final Integer from) {
        if (Objects.isNull(from)) {
            var booksByAuthorCountry = bookRepository.findAllByCountry(countryCode);
            booksByAuthorCountry.sort(getBookComparatorByYear());
            return booksByAuthorCountry;
        }
        return bookRepository.findAllByCountry(countryCode).stream()
          .filter(b -> Objects.nonNull(b.getYear()) && b.getYear() >= from)
          .sorted(getBookComparatorByYear())
          .toList();
    }

    private static Comparator<Book> getBookComparatorByYear() {
        return Comparator.comparing(Book::getYear,
          Comparator.nullsLast(Comparator.naturalOrder()));
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
    public Collection<Book> updateAllWithPublishYear() {
        log.info("Fetching all books for update with publish year");
        Iterable<Book> books = bookRepository.findAll();
        List<Book> booksToSave = StreamSupport.stream(books.spliterator(), false)
          .map(openLibraryClientAsync::getBookWithFirstPublishDateAsync)
          .map(cf -> cf.thenApply(this::getBookWithPublishedYear))
          .map(CompletableFuture::join)
          .toList();

        Iterable<Book> savedBooks = bookRepository.saveAll(booksToSave);
        List<Book> result = StreamSupport.stream(savedBooks.spliterator(), false)
          .toList();
        log.info(String.format("Books saved with updated publish year, cnt: %d", result.size()));
        return result;
    }

    private Book getBookWithPublishedYear(final BookWithPublishDate bookWithPublishDate) {
        final String workId = bookWithPublishDate.book().getWorkId();
        final String firstPublishDate = bookWithPublishDate.firstPublishDateWrapper().getFirstPublishDate();
        if (!StringUtils.hasLength(firstPublishDate)) {
            log.info(String.format("No first publish date found for book with workId: %s", workId));
            return bookWithPublishDate.book();
        }
        log.info(String.format("First publish date of %s found for book with workId: %s",
          firstPublishDate,
          workId));
        Matcher matcher = pattern.matcher(firstPublishDate);
        if (matcher.find()) {
            var year = matcher.group();
            log.info(String.format("Year %s extracted for book with workId %s", year, workId));
            bookWithPublishDate.book().setYear(Integer.valueOf(year));
        }
        return bookWithPublishDate.book();
    }
}
