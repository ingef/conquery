package com.bakdata.conquery.resources.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.apiv1.MeProcessor;
import com.bakdata.conquery.apiv1.MeProcessor.FrontendMeInformation;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * This endpoint is used to query information about the user itself. The
 * endpoint might be used to ask the backend to which groups a query can be
 * shared.
 */
@Path("me")
@Setter
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class MeResource extends HAuthorized {

	private final MeProcessor processor;

	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public FrontendMeInformation getUserInformation() {
		return processor.getUserInformation(subject.getUser());
	}
}
