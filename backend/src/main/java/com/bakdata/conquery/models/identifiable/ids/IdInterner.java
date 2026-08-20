package com.bakdata.conquery.models.identifiable.ids;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.identifiable.ids.IdUtil.Parser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.Data;

public class IdInterner implements Injectable {

	private final Map<Parser<?>, ParserIdInterner<?>> perParserInterner = new ConcurrentHashMap<>();

	public static IdInterner get(DeserializationContext context) throws JsonMappingException {
		return (IdInterner) context.findInjectableValue(IdInterner.class, null, null);
	}

	@SuppressWarnings("unchecked")
	public <ID extends Id<?, ?>> ParserIdInterner<ID> forParser(Parser<ID> parser) {
		return (ParserIdInterner<ID>) perParserInterner.computeIfAbsent(parser, k -> new ParserIdInterner<>(parser));
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(this.getClass(), this);
	}

	@Data
	public static class ParserIdInterner<ID extends Id<?, ?>> {
		private final Parser<ID> parser;
		private final Map<List<String>, ID> interned = new ConcurrentHashMap<>();

		public ID parse(List<String> components) {
			return interned.computeIfAbsent(components, parser::parse);
		}
	}
}
