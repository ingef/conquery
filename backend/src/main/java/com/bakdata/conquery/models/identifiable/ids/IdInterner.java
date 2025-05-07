package com.bakdata.conquery.models.identifiable.ids;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.identifiable.ids.IdUtil.Parser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;

public class IdInterner implements Injectable {
	
	private final Map<Parser<?>, ParserIdInterner<?>> perParserInterner = new ConcurrentHashMap<>();

	public static IdInterner get(DeserializationContext context) throws JsonMappingException {
		return (IdInterner) context.findInjectableValue(IdInterner.class, null, null);
	}

	@SuppressWarnings("unchecked")
	public <ID extends Id<?,?>> ParserIdInterner<ID> forParser(Parser<ID> parser) {
		return (ParserIdInterner<ID>) perParserInterner.computeIfAbsent(parser, k -> new ParserIdInterner<>());
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(this.getClass(), this);
	}

	public static class ParserIdInterner<ID extends Id<?,?>> {
		private final Map<List<String>, ID> interned = new ConcurrentHashMap<>();

		public ID putIfAbsent(List<String> components, ID id) {
			ID old = interned.putIfAbsent(components, id);

			if (old == null) {
				return id;
			}
			checkConflict(id, old);
			return old;
		}

		public static void checkConflict(Id<?,?> id, Id<?,?> cached) {
			if (!cached.equals(id)) {
				throw new IllegalStateException("The cached id '%s' (%s) conflicted with the new entry of '%s' (%s)"
														.formatted(cached, cached.getClass().getSimpleName(), id, id.getClass().getSimpleName()));
			}
		}

		public ID get(List<String> components) {
			return interned.get(components);
		}
	}
}
