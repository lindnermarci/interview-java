package hu.bca.library.feign;

import hu.bca.library.models.Book;
import hu.bca.library.models.BookWithPublishDate;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Log4j2
public class OpenLibraryClientAsync {

    private final OpenLibraryClient openLibraryClient;

    public OpenLibraryClientAsync(OpenLibraryClient openLibraryClient) {
        this.openLibraryClient = openLibraryClient;
    }

    @Async
    public CompletableFuture<BookWithPublishDate> getBookWithFirstPublishDateAsync(Book book){
        log.info(String.format("Calling open library to fetch first publish date for book, workId: %s", book.getWorkId()));
        return CompletableFuture.supplyAsync(() -> new BookWithPublishDate(book, openLibraryClient.getFirstPublishDate(book.getWorkId())));
    }

}