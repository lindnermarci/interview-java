package hu.bca.library.feign;

import hu.bca.library.models.Book;
import hu.bca.library.models.BookWithPublishDate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class OpenLibraryClientAsync {

    private final OpenLibraryClient openLibraryClient;

    public OpenLibraryClientAsync(OpenLibraryClient openLibraryClient) {
        this.openLibraryClient = openLibraryClient;
    }

    @Async
    public CompletableFuture<BookWithPublishDate> getBookWithFirstPublishDateAsync(Book book){
        return CompletableFuture.completedFuture(new BookWithPublishDate(book, openLibraryClient.getFirstPublishDate(book.getWorkId())));
    }

}