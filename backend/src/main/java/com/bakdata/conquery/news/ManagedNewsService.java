package com.bakdata.conquery.news;

import jakarta.validation.Validator;

import io.dropwizard.lifecycle.Managed;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public class ManagedNewsService implements Managed, NewsService {
    private final NewsConfiguration configuration;
    private final Validator validator;
    @Delegate
    private NewsService newsService;

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
