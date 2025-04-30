package com.bakdata.conquery.io.jersey;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdUtil.Parser;

public class IdParamConverter<T extends Id> implements ParamConverter<T> {

	//TODO inject storage
	private final Parser<T> parser;

	public IdParamConverter(Class<T> type) {
		parser = IdUtil.createParser(type);
	}

	@Override
	public T fromString(String value) {
		return parser.parse(value);
	}

	@Override
	public String toString(T value) {
		return value.toString();
	}
	
	public static class Provider implements ParamConverterProvider {

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
			if (Id.class.isAssignableFrom(rawType)) {
				return new IdParamConverter(rawType);
			}
			return null;
		}
		
	}
}
