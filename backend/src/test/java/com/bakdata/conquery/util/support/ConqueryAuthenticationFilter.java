package com.bakdata.conquery.util.support;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import lombok.Data;

import java.io.IOException;

@Data
class ConqueryAuthenticationFilter implements ClientRequestFilter {
    private final String token;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        // If none set to provided token
        if (requestContext.getHeaders().containsKey("Authorization")) {
            return;
        }

        requestContext.getHeaders().add("Authorization", "Bearer " + getToken());
    }
}
