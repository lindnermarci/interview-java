package hu.bca.library.feign;

import hu.bca.library.feign.model.FirstPublishDateWrapper;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(value = "openlibrary", url = "https://openlibrary.org/")
public interface OpenLibraryClient {

    @RequestMapping(method = RequestMethod.GET, value = "/works/{workId}.json", produces = "application/json")
    FirstPublishDateWrapper getFirstPublishDate(@PathVariable("workId") String workId);
}