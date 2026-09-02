package com.bakdata.conquery.quarkus.api;

import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/ping")
@Produces(MediaType.APPLICATION_JSON)
public class PingResource {

	@GET
	public Map<String, Object> ping() {
		return Map.of(
				"service", "conquery-backend-quarkus",
				"status", "ok",
				"timestamp", OffsetDateTime.now().toString()
		);
	}
}
