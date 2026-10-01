package com.bakdata.conquery.news;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

public final class NewsApi {

    private NewsApi() {
    }

    public static void register(
            ResourceConfig applicationApi,
            ResourceConfig adminApi,
            ResourceConfig adminUi,
            NewsService newsService) {

        applicationApi
                .register(newsBinder(newsService))
                .register(NewsResource.class);

        adminApi
                .register(newsBinder(newsService))
                .register(AdminNewsResource.class);

        adminUi.register(NewsUIResource.class);
    }

    private static AbstractBinder newsBinder(NewsService newsService) {
        return new AbstractBinder() {
            @Override
            protected void configure() {
                bind(newsService).to(NewsService.class);
            }
        };
    }
}
