package com.bakdata.conquery.apiv1.query.concept.filter;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdReferenceDeserializer;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@JsonDeserialize(using = FilterValue.FilterValueDeserializer.class)
@ToString(of = "value")
public class FilterValue<VALUE> {
	// TODO JsonIgnore "type" because there is no typing here anymore

	@NotNull
	@Nonnull
	@NsIdRef
	private Filter<VALUE> filter;

	@NotNull
	@Nonnull
	private VALUE value;

	public FilterNode<?> createNode() {
		return getFilter().createFilterNode(getValue());
	}


	/**
	 * This class deserializes the actual filter value depending on the resolved filter. This way there does not need to
	 * be a specific implementation of the filter value. It works like this:
	 *  1. The parser is advanced to the filter id
	 *  2. The filter id is resolved to the actual filter class using the NsIdReferenceDeserializer
	 *  3. The filter class is ask for the filter value type it expects
	 *  4. The parser is advanced to the start of the actual value / or the previously cached node of it
	 *  5. The actual filter value is deserialized using the type returned by the filter
	 *  6. Both filter and actual filter value are wrapped in the FilterValue class and returned
	 *
	 * @param <T> the actual filter value type
	 */
	public static class FilterValueDeserializer<T> extends JsonDeserializer<FilterValue<T>> {

		@Override
		public  FilterValue<T> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
			final NsIdReferenceDeserializer<FilterId, Filter<?>> nsIdDeserializer = new NsIdReferenceDeserializer<>(Filter.class, null, FilterId.class);

			Filter<T> filter = null;
			Class<T> valueClass = null;
			JsonNode valueNode = null;
			T value = null;

			while(jsonParser.nextToken() != JsonToken.END_OBJECT) {
				switch(jsonParser.currentName()) {
					case "filter":
						jsonParser.nextToken();
						filter = (Filter<T>) nsIdDeserializer.deserialize(jsonParser, deserializationContext);
						final TypeReference<? extends T> valueTypeReference = filter.getValueTypeReference();
						valueClass = (Class<T>) valueTypeReference.getType();
						break;
					case "value" :
						jsonParser.nextToken();
						if (valueClass == null) {
							// filter was not parsed yet
							valueNode = jsonParser.readValueAs(JsonNode.class);
						} else {
							value = jsonParser.readValueAs(valueClass);
						}
						break;
					default:
						deserializationContext.handleUnexpectedToken(FilterValue.class, jsonParser);
				}
			}

			if (valueClass == null) {
				throw new IllegalStateException("Unable to determine class of value member from filter");
			}
			if (valueNode == null && value == null) {
				// Value node might be null if the filter was parsed before the value
				throw new IllegalStateException("Unable to parse value node for filter");
			}

			if (value == null){
				// The "value" field was encountered before the "filter" field
				final JsonParser traverse = valueNode.traverse();
				// "Start" the parser
				traverse.nextToken();
				value = deserializationContext.readValue(traverse, valueClass);
			}

			return new FilterValue<T>(filter, (T) value);
		}
	}
}