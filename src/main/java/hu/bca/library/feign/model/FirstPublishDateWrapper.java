package hu.bca.library.feign.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FirstPublishDateWrapper {

    @JsonProperty("first_publish_date")
    private String firstPublishDate;
}
