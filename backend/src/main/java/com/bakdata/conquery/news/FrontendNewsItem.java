package com.bakdata.conquery.news;

import java.net.URI;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;

public record FrontendNewsItem(
    String id,
    @NotBlank
    String title,
    @NotBlank
    String description,
    URI link,
    LocalDate date) {
}
