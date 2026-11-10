package com.bakdata.conquery.news;

import java.net.URI;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FrontendNewsItem(
    @Size(max = 100, message = "ID must not exceed 100 characters")
    String id,
    @NotBlank
    String title,
    @NotBlank
    String description,
    URI link,
    LocalDate date) {
}
