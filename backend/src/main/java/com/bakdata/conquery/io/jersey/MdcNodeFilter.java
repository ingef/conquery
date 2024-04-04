package com.bakdata.conquery.io.jersey;

import java.io.IOException;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Priority(0)
public class MdcNodeFilter implements ContainerRequestFilter, ContainerResponseFilter {

	@Inject
	private MdcProvider.MdcNode nodeContainer;


	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		ConqueryMDC.setNode(nodeContainer.node());
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		ConqueryMDC.clearNode();
	}
}
