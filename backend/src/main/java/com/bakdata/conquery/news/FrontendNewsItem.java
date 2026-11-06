package com.bakdata.conquery.news;

import java.net.URI;
import java.time.LocalDate;

public record FrontendNewsItem(String id, String title, String description, URI link, LocalDate date) {
}
