package hu.bca.library.services.impl;

import hu.bca.library.feign.OpenLibraryClient;
import hu.bca.library.models.Author;
import hu.bca.library.models.Book;
import hu.bca.library.repositories.AuthorRepository;
import hu.bca.library.repositories.BookRepository;
import hu.bca.library.services.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BookServiceImpl implements BookService {

    private final Pattern pattern = Pattern.compile("\\b\\d{4}\\b");

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final OpenLibraryClient openLibraryClient;

    public BookServiceImpl(BookRepository bookRepository, AuthorRepository authorRepository, OpenLibraryClient openLibraryClient) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.openLibraryClient = openLibraryClient;
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

    private record BookYear(Book book, Integer year){};

    @Override
    public Collection<Book> updateAllWithYear() {
        Iterable<Book> books = bookRepository.findAll();
        for (Book book : books) {
            var publishDateWrapper = openLibraryClient.getFirstPublishDate(book.getWorkId());
            if (!StringUtils.hasLength(publishDateWrapper.getFirstPublishDate())) {
                continue;
            }
            Matcher matcher = pattern.matcher(publishDateWrapper.getFirstPublishDate());
            if (matcher.find()) {
                var year = matcher.group();
                book.setYear(Integer.valueOf(year));
            }
        }
        var iterable = bookRepository.saveAll(books);
        var result = new ArrayList<Book>();
        iterable.forEach(result::add);
        return result;
    }
}
