package com.bakdata.conquery.models.identifiable.ids;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.models.identifiable.ids.IdUtil.Parser;

public enum IIdInterner {
	
	INSTANCE;
	
	private final Map<Parser<?>, ParserIIdInterner<?>> perParserInterner = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public static <ID extends AId<?>> ParserIIdInterner<ID> forParser(Parser<ID> parser) {
		return (ParserIIdInterner<ID>) INSTANCE.perParserInterner.computeIfAbsent(parser, k -> new ParserIIdInterner<>());
	}

	public static class ParserIIdInterner<ID extends AId<?>> {
		private final Map<List<String>, ID> interned = new ConcurrentHashMap<>();

		public ID putIfAbsent(List<String> components, ID id) {
			return interned.putIfAbsent(components, id);
		}

		public ID get(List<String> components) {
			return interned.get(components);
		}
	}
}
