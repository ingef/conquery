package com.bakdata.conquery.models.concepts.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.stream.StreamSupport;

import javax.annotation.CheckForNull;
import javax.validation.Valid;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketEntry;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.google.common.util.concurrent.Striped;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class ConceptTreeConnector extends Connector {

	private static final long serialVersionUID = 1L;

	@NsIdRef
	@CheckForNull
	private Table table;

	@NsIdRef
	@CheckForNull
	private Column column = null;

	private CTCondition condition = null;

	@Valid
	@JsonManagedReference
	private List<Filter<?>> filters = new ArrayList<>();

	@ValidationMethod(message = "Table and Column usage are exclusive")
	public boolean tableXOrColumn() {
		return table == null ^ column == null;
	}


	@Override
	@JsonIgnore
	public Table getTable() {
		if (column != null) {
			return column.getTable();
		}

		return table;
	}

	@Override
	public List<Filter<?>> collectAllFilters() {
		return filters;
	}

	@Override
	public void calculateCBlock(CBlock cBlock, Bucket bucket) {

		final StringStore stringStore = findStringType(getColumn(), getConcept(), bucket);

		final int[][] mostSpecificChildren = new int[bucket.getNumberOfEvents()][];

		final ConceptTreeCache cache = getConcept().getCache(bucket.getImp().getId());

		final Striped<Lock> entityLock = Striped.lock(bucket.getBucketSize());

		StreamSupport.stream(bucket.entries().spliterator(), true)
					 .unordered()
					 .parallel()
					 .forEach(entry -> calculateEvent(entry, cBlock, stringType, cache, entityLock, mostSpecificChildren));

		cBlock.setMostSpecificChildren(mostSpecificChildren);



		if (cache != null) {
			log.trace(
					"Hits: {}, Misses: {}, Hits/Misses: {}, %Hits: {} (Up to now)",
					cache.getHits(),
					cache.getMisses(),
					(double) cache.getHits() / cache.getMisses(),
					(double) cache.getHits() / (cache.getHits() + cache.getMisses())
			);
		}
	}

	private StringStore findStringType(Column column, TreeConcept treeConcept, Bucket bucket) {

		// If we have a column and it is of string-type, we create indices and caches.
		if (column != null && bucket.getStores()[column.getPosition()] instanceof StringStore) {

			StringStore stringStore = (StringStore) bucket.getStores()[column.getPosition()];

			// Create index and insert into Tree.
			TreeChildPrefixIndex.putIndexInto(treeConcept);

			treeConcept.initializeIdCache(stringStore, bucket.getImp().getId());

			return stringStore;
		}
		// No column only possible if we have just one tree element!
		else if(treeConcept.countElements() == 1){
			return null;
		}
		else {
			throw new IllegalStateException(String.format("Cannot build tree over Connector[%s] without Column", getId()));
		}
	}

	@Override
	public TreeConcept getConcept() {
		return (TreeConcept) super.getConcept();
	}

	private void calculateEvent(BucketEntry entry, CBlock cBlock, StringStore stringStore, ConceptTreeCache cache, Striped<Lock> entityLock, int[][] mostSpecificChildren) {

		final Bucket bucket = entry.getBucket();
		final int event = entry.getEvent();
		final int entity = entry.getEntity();


		// Events without values are omitted
		// Events can also be filtered, allowing a single table to be used by multiple connectors.
		if (getColumn() != null && !bucket.has(event, getColumn())) {
			mostSpecificChildren[event] = Connector.NOT_CONTAINED;
			return;
		}

		String stringValue = "";
		int valueIndex = -1;

		if (stringStore != null) {
			valueIndex = bucket.getString(event, getColumn());
			stringValue = stringStore.getElement(valueIndex);
		}

		// Lazy evaluation of map to avoid allocations if possible.
		final CalculatedValue<Map<String, Object>> rowMap = new CalculatedValue<>(() -> bucket.calculateMap(event));
		ConceptTreeChild child = null;

		try {

			if (getCondition() != null && !getCondition().matches(stringValue, rowMap)) {
				mostSpecificChildren[event] = Connector.NOT_CONTAINED;
				return;
			}

			if(cache != null){
				child = cache.findMostSpecificChild(valueIndex, stringValue, rowMap);
			}
			else {
				child = getConcept().findMostSpecificChild(stringValue, rowMap);
			}
		}
		catch (ConceptConfigurationException ex) {
			log.error("Failed to resolve Bucket[{}](event = {}) against Concept[{}]", bucket.getId(), entry.getEvent(), this.getConcept().getId(), ex);
		}


		// All unresolved elements resolve to the root.
		if (child == null) {
			mostSpecificChildren[event] = getConcept().getPrefix();
			return;
		}

		// put path into event
		mostSpecificChildren[event] = child.getPrefix();

		final Lock lock = entityLock.get(entity);

		try {
			lock.lock();

			// also add concepts into bloom filter of entity cblock.
			ConceptTreeNode<?> it = child;
			while (it != null) {
				cBlock.addIncludedConcept(entry.getEntity(), it);
				it = it.getParent();
			}
		}
		finally {
			lock.unlock();
		}

	}

}
