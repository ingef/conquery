package com.bakdata.conquery.io.jackson;

import java.io.IOException;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.ResourceConstants;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.jakarta.rs.cfg.EndpointConfigBase;
import com.fasterxml.jackson.jakarta.rs.cfg.ObjectReaderInjector;
import com.fasterxml.jackson.jakarta.rs.cfg.ObjectReaderModifier;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.RequiredArgsConstructor;

public class PathParamInjector implements ContainerRequestFilter {

	@Inject
	public DatasetRegistry registry;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		ObjectReaderInjector.set(new Modifier(requestContext.getUriInfo().getPathParameters(), registry));
	}

	@RequiredArgsConstructor
	public static class Modifier extends ObjectReaderModifier implements Injectable {

		private final MultivaluedMap<String, String> pathParams;

		public final DatasetRegistry registry;

		@Override
		public ObjectReader modify(EndpointConfigBase<?> endpoint, MultivaluedMap<String, String> httpHeaders, JavaType resultType, ObjectReader reader, JsonParser p)
				throws IOException {
			return this.injectIntoNew(reader);
		}

		@Override
		public MutableInjectableValues inject(MutableInjectableValues values) {
			if (pathParams.containsKey(ResourceConstants.DATASET)) {
				final DatasetId datasetId = DatasetId.Parser.INSTANCE.parse(pathParams.getFirst(ResourceConstants.DATASET));
				values.add(DatasetId.class, datasetId);
				if (registry != null) {
					registry.get(datasetId).getInjectables().forEach(i -> i.inject(values));
				}
			}
			return values;
		}

	}
}
