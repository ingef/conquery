package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.google.common.collect.Multimap;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class NamespacedStorageImpl extends ConqueryStorageImpl implements NamespacedStorage {

	protected final Environment environment;
	protected SingletonStore<Dataset> dataset;
	protected KeyIncludingStore<IId<Dictionary>, Dictionary> dictionaries;
	protected IdentifiableStore<Import> imports;
	protected IdentifiableStore<Concept<?>> concepts;

	/**
	 * true if imports need to be registered with {@link Connector#addImport(Import)}.
	 */
	private final boolean registerImports;

	public NamespacedStorageImpl(Validator validator, StorageConfig config, File directory, boolean registerImports) {
		super(validator,config);
		this.registerImports = registerImports;
		this.environment = Environments.newInstance(directory, config.getXodus().createConfig());
		
	}

	@Override
	protected void createStores(Multimap<Environment, KeyIncludingStore<?,?>> environmentToStores) {
		dataset = StoreInfo.DATASET.<Dataset>singleton(getConfig(), environment, getValidator())
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

		if(ConqueryConfig.getInstance().getStorage().isUseWeakDictionaryCaching()) {
			dictionaries =	StoreInfo.DICTIONARIES.weakBig(getConfig(), environment, getValidator(), getCentralRegistry());
		}
		else {
			dictionaries =	StoreInfo.DICTIONARIES.big(getConfig(), environment, getValidator(), getCentralRegistry());
		}

		concepts =	StoreInfo.CONCEPTS.<Concept<?>>identifiable(getConfig(), environment, getValidator(), getCentralRegistry())
			.onAdd(concept -> {
				Dataset ds = centralRegistry.resolve(
					concept.getDataset() == null
						? concept.getId().getDataset()
						: concept.getDataset()
				);
				concept.setDataset(ds.getId());

				concept.initElements(validator);

				concept.getSelects().forEach(centralRegistry::register);
				for (Connector c : concept.getConnectors()) {
					centralRegistry.register(c);
					c.collectAllFilters().forEach(centralRegistry::register);
					c.getSelects().forEach(centralRegistry::register);
				}
				//add imports of table
				if(registerImports) {
					for (Import imp : getAllImports()) {
						for (Connector con : concept.getConnectors()) {
							if (con.getTable().getId().equals(imp.getTable())) {
								con.addImport(imp);
							}
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
		imports = StoreInfo.IMPORTS.<Import>identifiable(getConfig(), environment, getValidator(), getCentralRegistry())
			.onAdd(imp-> {
				imp.loadExternalInfos(this);

				if (registerImports) {
					for (Concept<?> c : getAllConcepts()) {
						for (Connector con : c.getConnectors()) {
							if (con.getTable().getId().equals(imp.getTable())) {
								con.addImport(imp);
							}
						}
					}
				}

				centralRegistry.register(imp);

				for (ImportColumn column : imp.getColumns()) {
					centralRegistry.register(column);
				}
			});

		// Order is important here
		environmentToStores.putAll(environment, List.of(
			dataset, 
			dictionaries, 
			concepts, 
			imports));
	}
	
	@Override
	public String getStorageOrigin() {
		return environment.getLocation();
	}

	@Override
	public Dataset getDataset() {
		return dataset.get();
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
	public Dictionary getDictionary(DictionaryId id) {
		return dictionaries.get(id);
	}

	@Override
	public DirectDictionary getPrimaryDictionary() {
		return new DirectDictionary(dictionaries.get(ConqueryConstants.getPrimaryDictionary(getDataset())));
	}

	@Override
	@SneakyThrows(JSONException.class)
	public void updateDictionary(Dictionary dict) {
		dictionaries.update(dict);
		for(Import imp : getAllImports()) {
			imp.loadExternalInfos(this);
		}
	}

	@Override
	public void removeDictionary(DictionaryId id) {
		dictionaries.remove(id);
	}

	@Override
	public Dictionary computeDictionary(DictionaryId id) {
		Dictionary e = getDictionary(id);
		if (e == null) {
			e = new MapDictionary(id.getDataset(), id.getDictionary());
			updateDictionary(e);
		}
		return e;
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
	public Collection<Import> getAllImports() {
		return imports.getAll();
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
	public Collection<Concept<?>> getAllConcepts() {
		return concepts.getAll();
	}
}
