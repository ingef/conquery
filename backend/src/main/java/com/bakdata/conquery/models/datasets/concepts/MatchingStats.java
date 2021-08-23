package com.bakdata.conquery.models.datasets.concepts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.*;

@Getter
@Setter
public class MatchingStats {

	private Map<WorkerId, Entry> entries = new HashMap<>();
	@JsonIgnore
	private transient CDateRange span;
	@JsonIgnore
	private transient long numberOfEvents = -1;

	@JsonIgnore
	private transient long numberOfEntities = -1;

	public synchronized long countEvents() {
		if (numberOfEvents == -1L) {
			numberOfEvents = entries.values().stream().mapToLong(Entry::getNumberOfEvents).sum();
		}
		return numberOfEvents;
	}


	public synchronized long countEntities() {
		if (numberOfEntities == -1L) {
			numberOfEntities = entries.values().stream().map(Entry::getFoundEntities).mapToLong(Set::size).sum();
		}
		return numberOfEntities;
	}
	public synchronized CDateRange spanEvents() {
		if (span == null) {
			span = entries.values().stream().map(Entry::getSpan).reduce(CDateRange.all(), CDateRange::spanClosed);
		}
		return span;
	}

	public void updateEntry(WorkerId source, Entry entry) {
		entries.put(source, entry);
		numberOfEvents = -1;
		span = null;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Entry {
		private long numberOfEvents;
		@JsonSerialize(contentUsing = IntSetSerializer.class)
		@JsonDeserialize(contentUsing = IntSetDeserializer.class)
		private IntSet foundEntities ;//= new IntOpenHashSet();
		private CDateRange span;


		public void addEvent(Table table, Bucket bucket, int event, int entityForEvent) {
			numberOfEvents++;
			if(foundEntities==null)
				foundEntities = new IntOpenHashSet();
			foundEntities.add(entityForEvent);

			for (Column c : table.getColumns()) {
				if (!c.getType().isDateCompatible()) {
					continue;
				}

				if (!bucket.has(event, c)) {
					continue;
				}

				final CDateRange time = bucket.getAsDateRange(event, c);
				span = time.spanClosed(span);
			}
		}
	}



	public static class IntSetSerializer extends JsonSerializer<IntSet> {
		@Override
		public void serialize(IntSet value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			final int[] ints = value.toIntArray();
			gen.writeArray(ints, 0, ints.length);
		}
	}

	public static class IntSetDeserializer extends JsonDeserializer<IntSet> {

		@Override
		public IntSet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			return IntSet.of(p.readValueAs(int[].class));
		}
	}
}
