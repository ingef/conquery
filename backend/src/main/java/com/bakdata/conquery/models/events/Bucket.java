package com.bakdata.conquery.models.events;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.bakdata.conquery.models.events.stores.root.DecimalStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.bakdata.conquery.models.events.stores.root.RealStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.preproc.PreprocessedData;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.validation.ValidationMethod;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Contains data from possibly multiple entities, loaded in a single import.
 */
@Slf4j
@FieldNameConstants
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@AllArgsConstructor
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator}, access = AccessLevel.PROTECTED)
public class Bucket extends IdentifiableImpl<BucketId> implements NamespacedIdentifiable<BucketId> {

	@ToString.Include
	@Min(0)
	private final int bucket;
	/**
	 * start of each Entity in {@code stores}.
	 */
	private final Object2IntMap<String> start;
	/**
	 * Number of events per Entity in {@code stores}.
	 */
	private final Object2IntMap<String> ends;
	private final int numberOfEvents;
	@ToString.Include
	private final ImportId imp;

	@JsonManagedReference
	@Setter(AccessLevel.PROTECTED)
	private ColumnStore[] stores;

	public static Bucket fromPreprocessed(Table table, PreprocessedData container, Import imp) {
		final ColumnStore[] storesSorted = sortColumns(table, container.getStores());
		final int numberOfEvents = container.getEnds().values().stream().mapToInt(i -> i).max().orElse(0);

		return new Bucket(container.getBucketId(),
						  new Object2IntOpenHashMap<>(container.getStarts()),
						  new Object2IntOpenHashMap<>(container.getEnds()),
						  numberOfEvents,
						  imp.getId(),
						  storesSorted
		);
	}

	private static ColumnStore[] sortColumns(Table table, Map<String, ColumnStore> stores) {
		return Arrays.stream(table.getColumns())
					 .map(Column::getName)
					 .map(stores::get)
					 .map(Objects::requireNonNull)
					 .toArray(ColumnStore[]::new);
	}

	@JsonIgnore
	@ValidationMethod(message = "Number of events does not match the length of some stores.")
	public boolean isNumberOfEventsEqualsNumberOfStores() {
		return Arrays.stream(stores).allMatch(columnStore -> columnStore.getLines() == getNumberOfEvents());
	}

	@Override
	public BucketId createId() {
		return new BucketId(imp, bucket);
	}

	/**
	 * Iterate entities
	 */
	public Collection<String> entities() {
		return ImmutableSet.copyOf(start.keySet());
	}

	public boolean containsEntity(String entity) {
		return start.containsKey(entity);
	}

	public int getEntityStart(String entityId) {
		return start.getInt(entityId);
	}

	public int getEntityEnd(String entityId) {
		return ends.getInt(entityId);
	}

	public final boolean has(int event, Column column) {
		return getStore(column).has(event);
	}

	public ColumnStore getStore(@NotNull Column column) {
		return stores[column.getPosition()];
	}

	public String getString(int event, @NotNull Column column) {
		return ((StringStore) getStore(column)).getString(event);
	}

	public long getInteger(int event, @NotNull Column column) {
		return ((IntegerStore) getStore(column)).getInteger(event);
	}

	public boolean getBoolean(int event, @NotNull Column column) {
		return ((BooleanStore) getStore(column)).getBoolean(event);
	}

	public double getReal(int event, @NotNull Column column) {
		return ((RealStore) getStore(column)).getReal(event);
	}

	public BigDecimal getDecimal(int event, @NotNull Column column) {
		return ((DecimalStore) getStore(column)).getDecimal(event);
	}

	public BigDecimal getMoney(int event, @NotNull Column column) {
		return ((MoneyStore) getStore(column)).getMoney(event);
	}

	public int getDate(int event, @NotNull Column column) {
		return ((DateStore) getStore(column)).getDate(event);
	}

	public boolean eventIsContainedIn(int event, ValidityDate validityDate, CDateSet dateRanges) {
		final CDateRange dateRange = validityDate.getValidityDate(event, this);

		if (dateRange == null) {
			return false;
		}

		return dateRanges.intersects(dateRange);
	}

	public CDateRange getAsDateRange(int event, Column column) {
		return switch (column.getType()) {
			case DATE -> CDateRange.exactly(((DateStore) getStore(column)).getDate(event));
			case DATE_RANGE -> getDateRange(event, column);
			default -> throw new IllegalStateException("Column is not of DateCompatible type.");
		};
	}

	public CDateRange getDateRange(int event, Column column) {
		return ((DateRangeStore) getStore(column)).getDateRange(event);
	}

	public Object createScriptValue(int event, @NotNull Column column) {
		return getStore(column).createScriptValue(event);
	}

	public IntFunction<Map<String, Object>> mapCalculator() {
		Column[] columns = getTable().resolve().getColumns();

		return event -> calculateMap(event, stores, columns);
	}

	@JsonIgnore
	public TableId getTable() {
		return imp.getTable();
	}

	private static Map<String, Object> calculateMap(int event, ColumnStore[] stores, Column[] columns) {
		final Map<String, Object> out = new HashMap<>(stores.length);

		for (int i = 0; i < stores.length; i++) {
			final ColumnStore store = stores[i];
			if (!store.has(event)) {
				continue;
			}
			out.put(columns[i].getName(), store.createScriptValue(event));
		}

		return out;
	}

	@JsonIgnore
	@Override
	public DatasetId getDataset() {
		return getTable().getDataset();
	}

	public ColumnStore getStore(@NotNull String storeName) {
		return getStore(getTable().resolve().getColumnByName(storeName));
	}
}