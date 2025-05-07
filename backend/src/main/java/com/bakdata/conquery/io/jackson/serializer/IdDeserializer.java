package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdInterner;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
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
public class IdDeserializer<ID extends Id<?, ?>> extends JsonDeserializer<ID> implements ContextualDeserializer {

	private Class<ID> idClass;
	private IdUtil.Parser<ID> idParser;
	private boolean isNamespacedId;

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
		JavaType type = Optional.ofNullable(ctxt.getContextualType())
								.orElseGet(Optional.ofNullable(property).map(BeanProperty::getType)::get);

		while (type.isContainerType()) {
			type = type.getContentType();
		}
		Class<Id> idClass = (Class<Id>) type.getRawClass();
		IdUtil.Parser<Id<Identifiable<?, ?>, ?>> parser = IdUtil.createParser((Class) idClass);

		return new IdDeserializer(
				idClass,
				parser,
				//we only need to check for the dataset prefix if the id requires it
				NamespacedId.class.isAssignableFrom(idClass)
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ID deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		JsonToken currentToken = parser.getCurrentToken();
		if (currentToken != JsonToken.VALUE_STRING) {
			return (ID) ctxt.handleUnexpectedToken(Id.class, currentToken, parser, "name references should be strings. Was: " + currentToken);
		}
		String text = parser.getText();

		// We need to assign resolvers for namespaced and meta ids because meta-objects might reference namespaced objects (e.g. ExecutionsId)
		NamespacedStorageProvider namespacedStorageProvider = NamespacedStorageProvider.getInjected(ctxt);
		MetaStorage metaStorage = MetaStorage.getInjected(ctxt);

		try {
			final ID id = deserializeId(text, idParser, isNamespacedId, ctxt);

			setResolver(id, metaStorage, namespacedStorageProvider);

			return id;
		}
		catch (Exception e) {
			return (ID) ctxt.handleWeirdStringValue(idClass, text, "Could not parse `" + idClass.getSimpleName() + "` from `" + text + "`: " + e.getMessage());
		}
	}

	public static void setResolver(Id<?, ?> id, MetaStorage metaStorage, NamespacedStorageProvider namespacedStorageProvider) {
		// Set resolvers in this id and subIds
		final Set<Id<?, ?>> ids = new HashSet<>();
		id.collectIds(ids);

		for (Id<?, ?> subId : ids) {
			// NamespacedIds always recur to the root, which is the DatasetId
			if (subId instanceof NamespacedId<?> nsId) {
				nsId.setDomain(namespacedStorageProvider);
			}
			if (subId instanceof MetaId<?> metaId) {
				metaId.setDomain(metaStorage);
			}
		}
	}

	public static <ID extends Id> ID deserializeId(String text, IdUtil.Parser<ID> idParser, boolean checkForInjectedPrefix, DeserializationContext ctx)
			throws JsonMappingException {

		List<String> components = checkForInjectedPrefix ?
								  IdUtil.Parser.asComponents(findDatasetName(ctx), text) :
								  IdUtil.Parser.asComponents(text);


		IdInterner interner = IdInterner.get(ctx);

		if (interner == null) {
			// Parse directly, as no interner is available
			return idParser.parse(components);
		}

		IdInterner.ParserIdInterner<ID> idParserIdInterner = interner.forParser(idParser);
		ID id = idParserIdInterner.get(components);

		if (id != null) {
			// Return cached id
			return id;
		}

		// Parse and cache
		id = idParser.parse(components);
		idParserIdInterner.putIfAbsent(components, id);

		return id;
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


}
