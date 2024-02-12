package hu.bca.library.repositories;

import hu.bca.library.models.Book;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends CrudRepository<Book, Long> {

  @Query(value = "SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.authors a WHERE a.country = :countryCode")
  List<Book> findAllByCountry(@Param("countryCode") String countryCode);
}
