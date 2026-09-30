package com.bakdata.conquery.news;

import io.dropwizard.lifecycle.Managed;
import jakarta.validation.Validator;
import lombok.experimental.Delegate;

public class ManagedNewsService implements Managed, NewsService {
    private final NewsConfiguration configuration;
    private final Validator validator;
    @Delegate
    private NewsService newsService;

    public ManagedNewsService(NewsConfiguration configuration, Validator validator) {
        this.configuration = configuration;
        this.validator = validator;
    }

    @Override
    public void start() throws Exception {
        this.newsService = new FileSystemNewsService(configuration.folder(), validator);
    }

    @Override
    public void stop() throws Exception {
        if (newsService != null) {
            newsService.close();
        }
    }
}
