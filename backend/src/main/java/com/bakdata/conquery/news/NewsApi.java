package com.bakdata.conquery.news;

import java.time.Clock;

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
        NewsPublicationPolicy publicationPolicy = new NewsPublicationPolicy(Clock.systemDefaultZone());

        applicationApi
                .register(newsBinder(newsService, publicationPolicy))
                .register(NewsResource.class);

        adminApi
                .register(newsBinder(newsService, publicationPolicy))
                .register(AdminNewsResource.class);

        adminUi.register(NewsUIResource.class);
    }

    private static AbstractBinder newsBinder(NewsService newsService, NewsPublicationPolicy publicationPolicy) {
        return new AbstractBinder() {
            @Override
            protected void configure() {
                bind(newsService).to(NewsService.class);
                bind(publicationPolicy).to(NewsPublicationPolicy.class);
            }
        };
    }
}
