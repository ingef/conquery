package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.validation.Validator;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
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
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.google.common.base.Stopwatch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class NamespacedStorageImpl extends ConqueryStorageImpl implements NamespacedStorage {

	protected final SingletonStore<Dataset> dataset;
	protected final IdentifiableStore<Dictionary> dictionaries;
	protected IdentifiableStore<Import> imports;
	protected final IdentifiableStore<Concept<?>> concepts;


	public NamespacedStorageImpl(Validator validator, StorageConfig config, File directory) {
		super(
			validator,
			config,
			directory
		);
		
		log.info("Loading storage from {}", directory);
		Stopwatch all = Stopwatch.createStarted();

		this.dataset =	new SingletonStore<>(StoreInfo.DATASET.cached(this)) {
			@Override
			protected void onValueAdded(Dataset ds) {
				centralRegistry.register(ds);
				for(Table t:ds.getTables().values()) {
					centralRegistry.register(t);
					for (Column c : t.getColumns()) {
						centralRegistry.register(c);
					}
				}
			}

			@Override
			protected void onValueRemoved(Dataset ds) {
				for(Table t:ds.getTables().values()) {
					for (Column c : t.getColumns()) {
						centralRegistry.remove(c);
					}
					centralRegistry.remove(t);
				}
				centralRegistry.remove(ds);
			}
		};
		this.dictionaries =	StoreInfo.DICTIONARIES.big(this);
		
		this.concepts =	new IdentifiableStore<Concept<?>>(centralRegistry, StoreInfo.CONCEPTS.cached(this)) {
			@Override
			protected void addToRegistry(CentralRegistry centralRegistry, Concept<?> concept) throws ConfigurationException, JSONException {
				if (concept.getDataset() == null) {
					Dataset ds = centralRegistry.resolve(concept.getId().getDataset());
					concept.setDataset(ds.getId());
					ds.addConcept(concept);
				}
				concept.initElements(validator);
				for (Connector c : concept.getConnectors()) {
					centralRegistry.register(c);
					c.getAllFilters().forEach(centralRegistry::register);
				}
			}

			@Override
			protected void removeFromRegistry(CentralRegistry centralRegistry, Concept<?> concept) {
				//see #146  remove from Dataset.concepts
				for(Connector c:concept.getConnectors()) {
					c.getAllFilters().stream().map(Filter::getId).forEach(centralRegistry::remove);
					centralRegistry.remove(c.getId());
				}
			}


		};

		log.info("Loaded complete storage within {}", all.stop());
	}

	@Override
	public void stopStores() throws IOException {
		dataset.close();
		dictionaries.close();
		imports.close();
		concepts.close();
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
		imports.get(id);
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
