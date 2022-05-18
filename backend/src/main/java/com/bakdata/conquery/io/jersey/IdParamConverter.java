package com.bakdata.conquery.io.jersey;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.AId.Parser;

public class IdParamConverter<T extends AId<?>> implements ParamConverter<T> {

	private final Parser<T> parser;

	public IdParamConverter(Class<T> type) {
		parser = AId.createParser(type);
	}

	@Override
	public T fromString(String value) {
		return parser.parse(value);
	}

	@Override
	public String toString(T value) {
		return value.toString();
	}
	
	public static enum Provider implements ParamConverterProvider {
		INSTANCE;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
			if (AId.class.isAssignableFrom(rawType)) {
				return new IdParamConverter(rawType);
			}
			return null;
		}
		
	}
}
