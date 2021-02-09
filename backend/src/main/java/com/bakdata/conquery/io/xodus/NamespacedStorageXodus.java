package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.validation.Validator;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.io.xodus.stores.Store;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.config.XodusStorageFactory;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormFrontendConfigProvider;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.google.common.collect.Multimap;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class NamespacedStorageXodus extends ConqueryStorageXodus implements NamespacedStorage {

	protected final Environment environment;
	/**
	 * true if imports need to be registered with {@link Connector#addImport(Import)}.
	 */
	private final boolean registerImports;

	/**
	 * Use weak caching for dictonaries.
	 */
	private final boolean useWeakDictionaryCaching;
	protected SingletonStore<Dataset> dataset;
	protected KeyIncludingStore<IId<Dictionary>, Dictionary> dictionaries;
	protected IdentifiableStore<Import> imports;
	protected IdentifiableStore<Table> tables;
	protected IdentifiableStore<SecondaryIdDescription> secondaryIds;
	protected IdentifiableStore<Concept<?>> concepts;

	public NamespacedStorageXodus(Validator validator, XodusStorageFactory config, File directory, boolean registerImports) {
		super(validator, config);
		this.registerImports = registerImports;
		this.environment = Environments.newInstance(directory, config.getXodus().createConfig());
		this.useWeakDictionaryCaching = config.isUseWeakDictionaryCaching();

	}

	@Override
	protected void createStores(Multimap<Environment, KeyIncludingStore<?, ?>> environmentToStores) {

		Function<StoreInfo, Store> normalStoreCreator = (storeInfo) -> getConfig().createStore(environment, validator, storeInfo);

		dataset = createDatasetStore(normalStoreCreator);

		secondaryIds = createSecondaryIdDescriptionStore(normalStoreCreator);

		tables = createTableStore(normalStoreCreator);

		if (useWeakDictionaryCaching) {
			dictionaries = StoreInfo.DICTIONARIES.identifiableCachedStore(getConfig().createBigWeakStore(environment,validator,StoreInfo.DICTIONARIES),getCentralRegistry());
		}
		else {
			dictionaries = StoreInfo.DICTIONARIES.identifiable(getConfig().createBigStore(environment,validator,StoreInfo.DICTIONARIES),getCentralRegistry());
		}

		concepts = createConceptStore(normalStoreCreator);

		imports = createImportStore(normalStoreCreator);

		// Order is important here
		environmentToStores.putAll(environment, List.of(
				dataset,
				secondaryIds,
				tables,
				dictionaries,
				concepts,
				imports
		));
	}

	@Override
	public Collection<Import> getAllImports() {
		return imports.getAll();
	}

	@Override
	public Collection<Concept<?>> getAllConcepts() {
		return concepts.getAll();
	}


	@Override
	public String getStorageOrigin() {
		return environment.getLocation();
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void updateDataset(Dataset dataset) {
		this.dataset.update(dataset);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void addDictionary(Dictionary dict) {
		dictionaries.add(dict);
	}

	@Override
	public EncodedDictionary getPrimaryDictionary() {
		return new EncodedDictionary(
				dictionaries.get(ConqueryConstants.getPrimaryDictionary(getDataset()))
				, StringTypeEncoded.Encoding.UTF8
		);
	}

	@Override
	public Dataset getDataset() {
		return dataset.get();
	}

	@Override
	public void removeDictionary(DictionaryId id) {
		dictionaries.remove(id);
	}


	@Override
	public Dictionary getDictionary(DictionaryId id) {
		return dictionaries.get(id);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void updateDictionary(Dictionary dict) {
		dictionaries.update(dict);
		for (Import imp : getAllImports()) {
			imp.loadExternalInfos(this);
		}
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void addImport(Import imp) {
		imports.add(imp);
	}

	@Override
	public Import getImport(ImportId id) {
		return imports.get(id);
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void updateImport(Import imp) {
		imports.update(imp);
	}

	@Override
	public void removeImport(ImportId id) {
		imports.remove(id);
	}

	@Override
	public Concept<?> getConcept(ConceptId id) {
		return concepts.get(id);
	}

	@Override
	public boolean hasConcept(ConceptId id) {
		return concepts.get(id) != null;
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void updateConcept(Concept<?> concept) {
		concepts.update(concept);
	}

	@Override
	public void removeConcept(ConceptId id) {
		concepts.remove(id);
	}

	@Override
	public List<Table> getTables() {
		return new ArrayList<>(tables.getAll());
	}

	@Override
	public Table getTable(TableId tableId) {
		return tables.get(tableId);
	}

	@SneakyThrows({JSONException.class})
	@Override
	public void addTable(Table table) {
		tables.add(table);
	}

	@Override
	public void removeTable(TableId table) {
		tables.remove(table);
	}

	@Override
	public List<SecondaryIdDescription> getSecondaryIds() {
		return new ArrayList<>(secondaryIds.getAll());
	}

	@Override
	public SecondaryIdDescription getSecondaryId(SecondaryIdDescriptionId descriptionId) {
		return secondaryIds.get(descriptionId);
	}

	@SneakyThrows({JSONException.class})
	@Override
	public void addSecondaryId(SecondaryIdDescription secondaryIdDescription) {
		secondaryIds.add(secondaryIdDescription);
	}

	@Override
	public synchronized void removeSecondaryId(SecondaryIdDescriptionId secondaryIdDescriptionId) {
		secondaryIds.remove(secondaryIdDescriptionId);
	}
}
