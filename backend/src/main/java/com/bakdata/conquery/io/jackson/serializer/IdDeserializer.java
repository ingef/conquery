package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NsIdResolver;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class IdDeserializer<ID extends Id<?>> extends JsonDeserializer<ID> implements ContextualDeserializer {

	private Class<ID> idClass;
	private IdUtil.Parser<ID> idParser;
	private boolean isNamespacedId;

	@SuppressWarnings("unchecked")
	@Override
	public ID deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
			return (ID) ctxt.handleUnexpectedToken(Id.class, parser.getCurrentToken(), parser, "name references should be strings");
		}
		String text = parser.getText();

		try {
			final ID id = deserializeId(text, idParser, isNamespacedId, ctxt);


			NsIdResolver nsIdResolver = null;
			MetaStorage metaStorage = null;
			final Class<?> activeView = ctxt.getActiveView();
			if (!isTestMode(ctxt, activeView)) {
				// We need to assign resolvers for namespaced and meta ids because meta-objects might reference namespaced objects (e.g. Executions)
				nsIdResolver = NsIdResolver.getResolver(ctxt);
				metaStorage = MetaStorage.get(ctxt);
			}

			setResolver(id, metaStorage, nsIdResolver);

			return id;
		}
		catch (Exception e) {
			return (ID) ctxt.handleWeirdStringValue(idClass, text, "Could not parse `" + idClass.getSimpleName() + "` from `" + text + "`: " + e.getMessage());
		}
	}

	public static void setResolver(Id<?> id, MetaStorage metaIdResolver, NsIdResolver nsIdResolver) {
		// Set resolvers in this id and subIds
		final HashSet<Id<?>> ids = new HashSet<>();
		id.collectIds(ids);
		for (Id<?> subId : ids) {
			if (subId.getIdResolver() != null) {
				// Ids are constructed of other ids that might already have a resolver set
				continue;
			}
			if (subId instanceof NamespacedId) {
				subId.setIdResolver(() -> nsIdResolver.resolve((Id<?> & NamespacedId) subId));
			}
			else if (metaIdResolver != null) {
				subId.setIdResolver(() -> metaIdResolver.resolve(subId));
			}
			// TODO Handle special Ids such as WorkerId, TableImportDescriptorId ?
		}
	}

	private static boolean isTestMode(DeserializationContext ctxt, Class<?> activeView) {
		return activeView != null && View.TestNoResolve.class.isAssignableFrom(ctxt.getActiveView());
	}

	public static <ID extends Id<?>> ID deserializeId(String text, IdUtil.Parser<ID> idParser, boolean checkForInjectedPrefix, DeserializationContext ctx)
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
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
		JavaType type = Optional.ofNullable(ctxt.getContextualType())
								.orElseGet(Optional.ofNullable(property).map(BeanProperty::getType)::get);

		while (type.isContainerType()) {
			type = type.getContentType();
		}
		Class<Id<?>> idClass = (Class<Id<?>>) type.getRawClass();
		IdUtil.Parser<Id<Identifiable<?>>> parser = IdUtil.createParser((Class) idClass);

		return new IdDeserializer(
				idClass,
				parser,
				//we only need to check for the dataset prefix if the id requires it
				NamespacedId.class.isAssignableFrom(idClass)
		);
	}
}
