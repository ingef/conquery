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
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
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
public class IdDeserializer<ID extends Id<?>> extends JsonDeserializer<ID> implements ContextualDeserializer {

	private Class<ID> idClass;
	private IdUtil.Parser<ID> idParser;
	private boolean isNamespacedId;
	private NsIdResolver nsIdResolver;
	private MetaStorage metaStorage;

	@SuppressWarnings("unchecked")
	@Override
	public ID deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
			return (ID) ctxt.handleUnexpectedToken(Id.class, parser.getCurrentToken(), parser, "name references should be strings");
		}
		String text = parser.getText();

		try {
			final ID id = deserializeId(text, idParser, isNamespacedId, ctxt);

			setResolver(id, metaStorage, nsIdResolver);

			return id;
		}
		catch (Exception e) {
			return (ID) ctxt.handleWeirdStringValue(idClass, text, "Could not parse `" + idClass.getSimpleName() + "` from `" + text + "`: " + e.getMessage());
		}
	}

	public static void setResolver(Id<?> id, MetaStorage metaStorage, NsIdResolver nsIdResolver) {
		// Set resolvers in this id and subIds
		final HashSet<Id<?>> ids = new HashSet<>();
		id.collectIds(ids);
		for (Id<?> subId : ids) {
			if (subId instanceof NamespacedId && nsIdResolver != null) {
				subId.setIdResolver(() -> nsIdResolver.get((Id<?> & NamespacedId) subId));
			}
			else if (metaStorage != null) {
				subId.setIdResolver(() -> metaStorage.get(subId));
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
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		JavaType type = Optional.ofNullable(ctxt.getContextualType())
								.orElseGet(Optional.ofNullable(property).map(BeanProperty::getType)::get);

		while (type.isContainerType()) {
			type = type.getContentType();
		}
		Class<Id<?>> idClass = (Class<Id<?>>) type.getRawClass();
		IdUtil.Parser<Id<Identifiable<?>>> parser = IdUtil.createParser((Class) idClass);

		NsIdResolver nsIdResolver = null;
		MetaStorage metaStorage = null;
		final Class<?> activeView = ctxt.getActiveView();
		if (!isTestMode(ctxt, activeView)) {

			if (NamespacedId.class.isAssignableFrom(idClass)) {
				nsIdResolver = NsIdResolver.getResolver(ctxt);
			}
			else if (WorkerId.class.isAssignableFrom(idClass)) {
				// TODO WorkerIds are not resolved yet
			}
			else {
				metaStorage = MetaStorage.get(ctxt);
			}
		}


		return new IdDeserializer(
				idClass,
				parser,
				//we only need to check for the dataset prefix if the id requires it
				NamespacedId.class.isAssignableFrom(idClass),
				nsIdResolver,
				metaStorage
		);
	}
}
