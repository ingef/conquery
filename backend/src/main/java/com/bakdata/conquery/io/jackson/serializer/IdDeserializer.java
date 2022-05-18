package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Optional;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdUtil.Parser;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class IdDeserializer<ID extends AId<?>> extends JsonDeserializer<ID> implements ContextualDeserializer {

	private Class<ID> idClass;
	private Parser<ID> idParser;
	private boolean checkForInjectedPrefix;

	@SuppressWarnings("unchecked")
	@Override
	public ID deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
			return (ID) ctxt.handleUnexpectedToken(AId.class, parser.getCurrentToken(), parser, "name references should be strings");
		}
		String text = parser.getText();

		try {
			return deserializeId(text, idParser, checkForInjectedPrefix, ctxt);
		}
		catch (Exception e) {
			return (ID) ctxt.handleWeirdStringValue(idClass, text, "Could not parse `" + idClass.getSimpleName() + "` from `" + text + "`: " + e.getMessage());
		}
	}

	public static <ID extends AId<?>> ID deserializeId(String text, Parser<ID> idParser, boolean checkForInjectedPrefix, DeserializationContext ctx)
			throws JsonMappingException {
		if (checkForInjectedPrefix) {
			//check if there was a dataset injected and if it is already a prefix
			String datasetName = findDatasetName(ctx);

			if (datasetName != null) {
				return idParser.parsePrefixed(datasetName, text);
			}
		}
		return idParser.parse(text);
	}

	private static String findDatasetName(DeserializationContext ctx) throws JsonMappingException {
		Dataset dataset = Jackson.findInjectable(ctx, Dataset.class);

		if (dataset != null) {
			return dataset.getName();
		}

		// Sometimes injected via @PathParam

		DatasetId id = Jackson.findInjectable(ctx, DatasetId.class);

		if (id != null) {
			return id.getName();
		}
		return null;
	}

	@Override
	public ID deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
		return this.deserialize(p, ctxt);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		JavaType type = Optional.ofNullable(ctxt.getContextualType())
								.orElseGet(Optional.ofNullable(property).map(BeanProperty::getType)::get);

		while (type.isContainerType()) {
			type = type.getContentType();
		}
		Class<AId<?>> idClass = (Class<AId<?>>) type.getRawClass();
		Parser<AId<Identifiable<?>>> parser = IdUtil.<AId<Identifiable<?>>>createParser((Class) idClass);

		return new IdDeserializer(
				idClass,
				parser,
				//we only need to check for the dataset prefix if the id requires it
				NamespacedId.class.isAssignableFrom(idClass)
		);
	}
}
