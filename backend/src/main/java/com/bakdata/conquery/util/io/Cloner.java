package com.bakdata.conquery.util.io;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.types.specific.StringType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Cloner {
	
	public static <T> ConqueryConfig clone(ConqueryConfig config, Map<Class<T> , T > injectables) {
		try {
			ObjectMapper mapper = Jackson.BINARY_MAPPER.copy();
			MutableInjectableValues injectableHolder = ((MutableInjectableValues)Jackson.BINARY_MAPPER.getInjectableValues());
			for(Entry<Class<T>, T> injectable : injectables.entrySet()) {
				
				injectableHolder.add(injectable.getKey(), injectable.getValue());
			}
			ConqueryConfig clone = mapper.readValue(
				Jackson.BINARY_MAPPER.writeValueAsBytes(config),
				ConqueryConfig.class
			);
			clone.setLoggingFactory(config.getLoggingFactory());
			return clone;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to clone a conquery config "+config, e);
		}
	}
	
	public static StringType clone(StringType type) {
		try {
			StringType clone = Jackson.BINARY_MAPPER.readValue(
				Jackson.BINARY_MAPPER.writeValueAsBytes(type),
				StringType.class
			);
			return clone;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to clone a type "+type, e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends IQuery> T clone(T query, Injectable injectable) {
		try {
			T clone = (T) injectable
				.injectInto(Jackson.BINARY_MAPPER)
				.readValue(
					Jackson.BINARY_MAPPER.writeValueAsBytes(query),
					IQuery.class
				);
			return clone;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to clone a query "+query, e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends CQElement> T clone(T element) {
		try {
			T clone = (T)Jackson.BINARY_MAPPER.readValue(
				Jackson.BINARY_MAPPER.writeValueAsBytes(element),
				CQElement.class
			);
			return clone;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to clone the CQElement "+element, e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends CQElement> T clone(T element, Injectable injectable) {
		try {
			T clone = (T)injectable
				.injectInto(Jackson.BINARY_MAPPER)
				.readValue(
					Jackson.BINARY_MAPPER.writeValueAsBytes(element),
					CQElement.class
				);
			return clone;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to clone the CQElement "+element, e);
		}
	}
}
