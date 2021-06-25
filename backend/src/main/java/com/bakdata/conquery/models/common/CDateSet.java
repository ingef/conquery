package com.bakdata.conquery.models.common;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.bakdata.conquery.util.DateFormats;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;
import com.google.common.collect.ForwardingCollection;
import com.google.common.math.IntMath;
import lombok.EqualsAndHashCode;

/**
 * (De-)Serializers are are registered programmatically because they depend on {@link com.bakdata.conquery.util.DateFormats}
 */
@EqualsAndHashCode
public class CDateSet {

	private static final Pattern PARSE_PATTERN = Pattern.compile("(\\{|,\\s*)((\\d{4}-\\d{2}-\\d{2})?/(\\d{4}-\\d{2}-\\d{2})?)");
	private final NavigableMap<Integer, CDateRange> rangesByLowerBound;
	private transient Set<CDateRange> asRanges;
	private transient Set<CDateRange> asDescendingSetOfRanges;

	public static CDateSet create() {
		return new CDateSet(new TreeMap<>());
	}
	
	public static CDateSet createFull() {
		CDateSet set = new CDateSet(new TreeMap<>());
		set.add(CDateRange.all());
		return set;
	}

	public static CDateSet create(CDateSet rangeSet) {
		CDateSet result = create();
		result.addAll(rangeSet);
		return result;
	}
	
	public static CDateSet create(CDateRange range) {
		CDateSet result = create();
		result.add(range);
		return result;
	}

	public static CDateSet create(Iterable<CDateRange> ranges) {
		CDateSet result = create();
		result.addAll(ranges);
		return result;
	}

	private CDateSet(NavigableMap<Integer, CDateRange> rangesByLowerCut) {
		this.rangesByLowerBound = rangesByLowerCut;
	}

	public Set<CDateRange> asRanges() {
		Set<CDateRange> result = asRanges;
		return (result == null) ? asRanges = new AsRanges(rangesByLowerBound.values()) : result;
	}


	final class AsRanges extends ForwardingCollection<CDateRange> implements Set<CDateRange> {

		final Collection<CDateRange> delegate;

		AsRanges(Collection<CDateRange> delegate) {
			this.delegate = delegate;
		}

		@Override
		protected Collection<CDateRange> delegate() {
			return delegate;
		}

		@Override
		public int hashCode() {
			int hashCode = 0;
			for (Object o : this) {
				hashCode += o != null ? o.hashCode() : 0;

				hashCode = ~~hashCode;
			}
			return hashCode;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (object instanceof Set) {
				Set<?> o = (Set<?>) object;

				try {
					return this.size() == o.size() && this.containsAll(o);
				}
				catch (NullPointerException | ClassCastException ignored) {
					return false;
				}
			}
			return false;
		}
	}

	public CDateRange rangeContaining(int value) {
		checkNotNull(value);
		Entry<Integer, CDateRange> floorEntry = rangesByLowerBound.floorEntry(value);
		if (floorEntry != null && floorEntry.getValue().contains(value)) {
			return floorEntry.getValue();
		}
		else {
			return null;
		}
	}

	/**
	 * Tests if the supplied {@link LocalDate} is contained by this Set.
	 * @param value the Date to check
	 * @return true iff any Set contains the value
	 */
	public boolean contains(LocalDate value) {
		return contains(CDate.ofLocalDate(value));
	}

	/**
	 * Tests if the supplied {@link CDate} is contained by this Set.
	 * @param value the Date to check
	 * @return true iff any Set contains the value
	 */
	public boolean contains(int value) {
		return rangeContaining(value) != null;
	}
	
	public boolean isEmpty() {
		return asRanges().isEmpty();
	}

	public void clear() {
		rangesByLowerBound.clear();
	}

	
	public void addAll(CDateSet other) {
		addAll(other.asRanges());
	}
	
	public void removeAll(CDateSet other) {
		removeAll(other.asRanges());
	}

	public boolean enclosesAll(Iterable<CDateRange> other) {
		for (CDateRange range : other) {
			if (!encloses(range)) {
				return false;
			}
		}
		return true;
	}

	public void addAll(Iterable<CDateRange> ranges) {
		for (CDateRange range : ranges) {
			add(range);
		}
	}

	public void removeAll(Iterable<CDateRange> ranges) {
		for (CDateRange range : ranges) {
			remove(range);
		}
	}
	
	public boolean intersects(CDateRange range) {
		checkNotNull(range);
		Entry<Integer, CDateRange> ceilingEntry = rangesByLowerBound.ceilingEntry(range.getMinValue());
		if (ceilingEntry != null && ceilingEntry.getValue().intersects(range)) {
			return true;
		}
		Entry<Integer, CDateRange> priorEntry = rangesByLowerBound.lowerEntry(range.getMinValue());
		return priorEntry != null && priorEntry.getValue().intersects(range);
	}

	public boolean encloses(CDateRange range) {
		checkNotNull(range);
		Entry<Integer, CDateRange> floorEntry = rangesByLowerBound.floorEntry(range.getMinValue());
		return floorEntry != null && floorEntry.getValue().encloses(range);
	}

	public CDateRange span() {
		Entry<Integer, CDateRange> firstEntry = rangesByLowerBound.firstEntry();
		if (firstEntry == null) {
			throw new NoSuchElementException();
		}
		return CDateRange.of(firstEntry.getValue().getMinValue(), rangesByLowerBound.lastEntry().getValue().getMaxValue());
	}

	public void add(CDateRange rangeToAdd) {
		checkNotNull(rangeToAdd);
		
		int lbToAdd = rangeToAdd.getMinValue();
		int ubToAdd = rangeToAdd.getMaxValue();

		Entry<Integer, CDateRange> entryBelowLB = rangesByLowerBound.lowerEntry(lbToAdd);
		if (entryBelowLB != null) {
			CDateRange rangeBelowLB = entryBelowLB.getValue();
			//left neighbor would be connected
			if (rangeBelowLB.getMaxValue() >= lbToAdd - 1) {
				lbToAdd = rangeBelowLB.getMinValue();
				//left neighbor encloses new range
				if (rangeBelowLB.getMaxValue() > ubToAdd) {
					ubToAdd = rangeBelowLB.getMaxValue();
				}
				
			}
		}

		Entry<Integer, CDateRange> entryBelowUB = rangesByLowerBound.floorEntry(ubToAdd + 1);
		if (entryBelowUB != null) {
			CDateRange rangeBelowUB = entryBelowUB.getValue();
			if (rangeBelowUB.getMaxValue() >= ubToAdd) {
				ubToAdd = rangeBelowUB.getMaxValue();
			}
		}

		// Remove ranges which are strictly enclosed.
		rangesByLowerBound.subMap(lbToAdd, IntMath.saturatedAdd(ubToAdd, 1)).clear();

		putRange(CDateRange.of(lbToAdd, ubToAdd));
	}
	
	public void remove(CDateRange rangeToRemove) {
		checkNotNull(rangeToRemove);

		Entry<Integer, CDateRange> entryBelowLB = rangesByLowerBound.lowerEntry(rangeToRemove.getMinValue());
		if (entryBelowLB != null) {
			CDateRange rangeBelowLB = entryBelowLB.getValue();
			//left neighbor intersects removed range => shorten it to everything before removed range
			if (rangeBelowLB.getMaxValue() >= rangeToRemove.getMinValue()) {
				putRange(CDateRange.of(rangeBelowLB.getMinValue(), rangeToRemove.getMinValue() - 1));
				
				//left neighbor reaches beyond removed range => have to add cut of right part
				if (rangeBelowLB.getMaxValue() > rangeToRemove.getMaxValue()) {
					putRange(CDateRange.of(rangeToRemove.getMaxValue() + 1, rangeBelowLB.getMaxValue()));
				}
			}
		}

		Entry<Integer, CDateRange> entryBelowUB = rangesByLowerBound.floorEntry(rangeToRemove.getMaxValue());
		if (entryBelowUB != null) {
			CDateRange rangeBelowUB = entryBelowUB.getValue();
			//if reaches beyond removed range => have to add cut of right part
			if (rangeBelowUB.getMaxValue() > rangeToRemove.getMaxValue()) {
				// { > }
				putRange(CDateRange.of(rangeToRemove.getMaxValue() + 1, rangeBelowUB.getMaxValue()));
			}
		}

		rangesByLowerBound.subMap(rangeToRemove.getMinValue(), IntMath.saturatedAdd(rangeToRemove.getMaxValue(),1)).clear();
	}

	private void putRange(CDateRange range) {
		rangesByLowerBound.put(range.getMinValue(), range);
	}


	public void maskedAdd(CDateRange toAdd, CDateSet mask){
		if(mask.isEmpty()){
			return;
		}

		if(mask.isAll()){
			add(toAdd);
			return;
		}

		if(toAdd.isAll()){
			addAll(mask);
			return;
		}

		if(toAdd.isExactly() && mask.contains(toAdd.getMinValue())){
			add(toAdd);
			return;
		}

		// Look for start and end of iteration.
		Integer search = null;

		if (toAdd.hasLowerBound()) {
			search = mask.rangesByLowerBound.floorKey(toAdd.getMinValue());
		}

		if(search == null) {
			search = mask.rangesByLowerBound.firstKey();
		}

		Integer searchEnd = null;

		if(toAdd.hasUpperBound()){
			searchEnd = mask.rangesByLowerBound.floorKey(toAdd.getMaxValue());
		}

		if(searchEnd == null){
			searchEnd = mask.rangesByLowerBound.lastKey();
		}

		while(search != null && search <= searchEnd) {
			final CDateRange range = mask.rangesByLowerBound.get(search);

			search = mask.rangesByLowerBound.higherKey(search);

			int min = range.getMinValue();
			int max = range.getMaxValue();

			if(max < toAdd.getMinValue()){
				continue;
			}

			if(min < toAdd.getMinValue()){
				min = toAdd.getMinValue();
			}

			if(max > toAdd.getMaxValue()){
				max = toAdd.getMaxValue();
			}

			// value was not contained
			if(min > max){
				continue;
			}

			add(CDateRange.of(min, max));
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		Joiner.on(", ").appendTo(sb, this.asRanges());
		sb.append('}');
		return sb.toString();
	}

	@JsonIgnore
	public boolean isAll() {
		if(this.rangesByLowerBound.isEmpty()) {
			return false;
		}
		return this.rangesByLowerBound.firstEntry().getValue().isAll();
	}

	/**
	 * test if any of the boundaries are open.
	 */
	@JsonIgnore
	public boolean isOpen() {
		if(this.rangesByLowerBound.isEmpty()) {
			return false;
		}

		// Since we might be all, just check if any of the boundaries are open.
		return rangesByLowerBound.firstEntry().getValue().isOpen() || rangesByLowerBound.lastEntry().getValue().isOpen();
	}

	public void retainAll(CDateSet retained) {
		if(retained.isEmpty()) {
			this.clear();
			return;
		}
		if(retained.isAll()) {
			return;
		}

		List<CDateRange> l = new ArrayList<>(retained.rangesByLowerBound.values());
		
		//remove all before the first range
		if(!l.get(0).isAtMost()) {
			this.remove(CDateRange.of(Integer.MIN_VALUE, l.get(0).getMinValue() - 1));
		}
		
		//remove all between ranges
		for(int i=0;i<l.size()-1;i++) {
			this.remove(CDateRange.of(l.get(i).getMaxValue() + 1, l.get(i+1).getMinValue() - 1));
		}
		
		//remove all after the last Range
		if(!l.get(l.size()-1).isAtLeast()) {
			this.remove(CDateRange.of(l.get(l.size()-1).getMaxValue() + 1, Integer.MAX_VALUE));
		}
	}
	
	public void retainAll(CDateRange retained) {
		if(retained.isAll()) {
			return;
		}

		//remove all before the range
		if(!retained.isAtMost()) {
			this.remove(CDateRange.of(Integer.MIN_VALUE, retained.getMinValue() - 1));
		}
		
		//remove all after the Range
		if(!retained.isAtLeast()) {
			this.remove(CDateRange.of(retained.getMaxValue() + 1, Integer.MAX_VALUE));
		}
	}

	/**
	 * Counts the number of days represented by this CDateSet.
	 * @return the number of days or null if there are infinite days in the set
	 */
	public Long countDays() {
		//if we have no entries we return zero days
		if(rangesByLowerBound.firstEntry() == null) {
			return 0L;
		}
		if(rangesByLowerBound.firstEntry().getValue().isOpen() || rangesByLowerBound.lastEntry().getValue().isOpen()) {
			return null;
		}
		long sum = 0;
		for(CDateRange r:this.asRanges()) {
			sum+=r.getMaxValue() - r.getMinValue() + 1;
		}
		return sum;
	}

	public int getMinValue() {
		return rangesByLowerBound.firstEntry().getValue().getMinValue();
	}

	public int getMaxValue() {
		return rangesByLowerBound.lastEntry().getValue().getMaxValue();
	}
	
	public static CDateSet parse(String value, DateFormats dateFormats) {
		List<CDateRange> ranges = PARSE_PATTERN
			.matcher(value)
			.results()
			.map(mr -> {
				try {
					return DateRangeParser.parseISORange(mr.group(2), dateFormats);
				}
				catch(Exception e) {
					throw new RuntimeException(e);
				}
			})
			.collect(Collectors.toList());
		return CDateSet.create(ranges);
	}
}