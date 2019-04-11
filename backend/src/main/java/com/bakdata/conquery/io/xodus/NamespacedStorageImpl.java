package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.validation.Validator;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.util.functions.Collector;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class NamespacedStorageImpl extends ConqueryStorageImpl implements NamespacedStorage {

	protected SingletonStore<Dataset> dataset;
	protected IdentifiableStore<Dictionary> dictionaries;
	protected IdentifiableStore<Import> imports;
	protected IdentifiableStore<Concept<?>> concepts;

	public NamespacedStorageImpl(Validator validator, StorageConfig config, File directory) {
		super(
			validator,
			config,
			directory
		);
	}
	
	@Override
	protected void createStores(Collector<KeyIncludingStore<?, ?>> collector) {
		this.dataset = StoreInfo.DATASET.<Dataset>singleton(this)
			.onAdd(ds -> {
				centralRegistry.register(ds);
				for(Table t:ds.getTables().values()) {
					centralRegistry.register(t);
					for (Column c : t.getColumns()) {
						centralRegistry.register(c);
					}
				}
			})
			.onRemove(ds -> {
				for(Table t:ds.getTables().values()) {
					for (Column c : t.getColumns()) {
						centralRegistry.remove(c);
					}
					centralRegistry.remove(t);
				}
				centralRegistry.remove(ds);
			});
		this.dictionaries =	StoreInfo.DICTIONARIES.big(this);
		
		this.concepts =	StoreInfo.CONCEPTS.<Concept<?>>identifiable(this)
			.onAdd(concept -> {
				Dataset ds = centralRegistry.resolve(
					concept.getDataset() == null
						? concept.getId().getDataset()
						: concept.getDataset()
				);
				concept.setDataset(ds.getId());
				ds.addConcept(concept);

				concept.initElements(validator);
				
				concept.getSelects().forEach(centralRegistry::register);
				for (Connector c : concept.getConnectors()) {
					centralRegistry.register(c);
					c.collectAllFilters().forEach(centralRegistry::register);
					c.getSelects().forEach(centralRegistry::register);
				}
				//add imports of table
				for(Import imp: getAllImports()) {
					for(Connector con : concept.getConnectors()) {
						if(con.getTable().getId().equals(imp.getTable())) {
							con.addImport(imp);
						}
					}
				}
			})
			.onRemove(concept -> {
				concept.getSelects().forEach(centralRegistry::remove);
				//see #146  remove from Dataset.concepts
				for(Connector c:concept.getConnectors()) {
					c.getSelects().forEach(centralRegistry::remove);
					c.collectAllFilters().stream().map(Filter::getId).forEach(centralRegistry::remove);
					centralRegistry.remove(c.getId());
				}
			});
		this.imports = StoreInfo.IMPORTS.<Import>identifiable(this)
			.onAdd(imp-> {
				imp.loadExternalInfos(this);
				for(Concept<?> c: getAllConcepts()) {
					for(Connector con : c.getConnectors()) {
						if(con.getTable().getId().equals(imp.getTable())) {
							con.addImport(imp);
						}
					}
				}
			});
		
		collector
			.collect(dataset)
			.collect(dictionaries)
			.collect(concepts)
			.collect(imports);
	}

	@Override
	public Dataset getDataset() {
		return dataset.get();
	}

	@Override
	public void updateDataset(Dataset dataset) throws JSONException {
		this.dataset.update(dataset);
	}

	@Override
	public void addDictionary(Dictionary dict) throws JSONException {
		dictionaries.add(dict);
	}

	@Override
	public Dictionary getDictionary(DictionaryId id) {
		return dictionaries.get(id);
	}

	@Override
	public Dictionary getPrimaryDictionary() {
		return dictionaries.get(ConqueryConstants.getPrimaryDictionary(getDataset()));
	}

	@Override
	public void updateDictionary(Dictionary dict) throws JSONException {
		dictionaries.update(dict);
	}

	@Override
	public void removeDictionary(DictionaryId id) {
		dictionaries.remove(id);
	}

	@Override
	public Dictionary computeDictionary(DictionaryId id) throws JSONException {
		Dictionary e = getDictionary(id);
		if (e == null) {
			e = new Dictionary(id);
			e.compress();
			updateDictionary(e);
		}
		return e;
	}

	@Override
	public void addImport(Import imp) throws JSONException {
		imports.add(imp);
	}

	@Override
	public Import getImport(ImportId id) {
		return imports.get(id);
	}

	@Override
	public Collection<Import> getAllImports() {
		return imports.getAll();
	}

	@Override
	public void updateImport(Import imp) throws JSONException {
		imports.update(imp);
	}

	@Override
	public void removeImport(ImportId id) {
		imports.remove(id);
	}

	@Override
	public Concept<?> getConcept(ConceptId id) {
		return Optional.ofNullable(concepts.get(id))
			.orElseThrow(() -> new NoSuchElementException("Could not find the concept " + id));
	}

	@Override
	public void updateConcept(Concept<?> concept) throws JSONException {
		concepts.update(concept);
	}

	@Override
	public void removeConcept(ConceptId id) {
		concepts.remove(id);
	}

	@Override
	public Collection<Concept<?>> getAllConcepts() {
		return concepts.getAll();
	}
}
