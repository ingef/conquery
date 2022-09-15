package com.bakdata.conquery.models.forms.arx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.DataType;
import org.deidentifier.arx.aggregates.HierarchyBuilderDate;
import org.deidentifier.arx.aggregates.HierarchyBuilderIntervalBased;
import org.deidentifier.arx.aggregates.HierarchyBuilderIntervalBased.Range;

/**
 * Helper classes that allow us to gather values for possible attribute hierarchies, while
 * we convert from the internal conquery result format to the ARX {@link org.deidentifier.arx.Data} format.
 */
public interface AttributeTypeBuilder {

	String SUPPRESSING_GRANULARITY = "*";

	long BUCKET_SIZE = 5;

	int GROUPS_PER_LEVEL = 2;


	String register(Object value);

	AttributeType build();

	/**
	 * Builds a two level hierarchy. Bottom level are the actual values. Top level is "*".
	 */
	@RequiredArgsConstructor
	class Flat implements AttributeTypeBuilder {

		private final Function<Object, String> printer;

		private final Set<String> values = new HashSet<>();

		@Override
		public String register(Object value) {
			String printValue = printer.apply(value);
			values.add(printValue);
			return printValue;
		}

		@Override
		public AttributeType build() {
			return AttributeType.Hierarchy.create(values.stream().map(v -> new String[]{v, "*"}).toArray(String[][]::new));
		}
	}

	@RequiredArgsConstructor
	class Date implements AttributeTypeBuilder {

		private final PrintSettings printSettings;
		private final Set<String> values = new HashSet<>();

		@Override
		public String register(Object value) {
			final String printValue = ResultType.DateT.INSTANCE.printNullable(printSettings, value);
			values.add(printValue);
			return printValue;
		}

		@Override
		public AttributeType build() {
			final AttributeType.Hierarchy build = HierarchyBuilderDate.create(
					DataType.DATE,
					HierarchyBuilderDate.Granularity.DAY_MONTH_YEAR,
					HierarchyBuilderDate.Granularity.MONTH_YEAR,
					HierarchyBuilderDate.Granularity.YEAR,
					HierarchyBuilderDate.Granularity.DECADE
			).build(values.stream().filter(Predicate.not(Strings::isNullOrEmpty)).toArray(String[]::new));

			// WORKAROUND: Add empty string handling to the date hierarchy
			final String[][] hierarchy = build.getHierarchy();
			for (int i = 0; i < hierarchy.length; i++) {
				// Extend the hierarchy by a suppressing granularity (*)
				String[] extended = Arrays.copyOf(hierarchy[i], hierarchy[i].length + 1);
				extended[extended.length - 1] = SUPPRESSING_GRANULARITY;
				hierarchy[i] = extended;
			}

			// Add hierarchy handling for empty values
			String[][] newHierarchy = Arrays.copyOf(hierarchy, hierarchy.length + 1);
			String[] emptyValueHierarchy = new String[hierarchy.length > 0 ? hierarchy[0].length : 0];
			Arrays.fill(emptyValueHierarchy, "");
			emptyValueHierarchy[emptyValueHierarchy.length - 1] = SUPPRESSING_GRANULARITY;
			newHierarchy[newHierarchy.length - 1] = emptyValueHierarchy;

			return AttributeType.Hierarchy.create(newHierarchy);
		}
	}

	@Slf4j
	class IntegerInterval implements AttributeTypeBuilder {

		/**
		 * Keeps track of the smallest value in the data
		 */
		private long min = Long.MAX_VALUE;

		/**
		 * Keeps track of the largest value in the data
		 */
		private long max = Long.MIN_VALUE;

		/**
		 * Tracks all data for hierarchy preparation step
		 */
		private final Set<String> values = new HashSet<>();

		@Override
		public String register(Object value) {

			if (value == null) {
				values.add(DataType.NULL_VALUE);
				return DataType.NULL_VALUE;
			}

			if (!(value instanceof Number)) {
				throw new IllegalArgumentException("Expected a " + Number.class + " type, but got " + value.getClass() + ".");
			}

			final long l = ((Number) value).longValue();
			min = Math.min(min, l);
			max = Math.max(max, l);

			final String returnVal = Long.toString(l);
			values.add(returnVal);
			return returnVal;
		}

		@Override
		public AttributeType.Hierarchy build() {
			final HierarchyBuilderIntervalBased<Long> builder = HierarchyBuilderIntervalBased.create(
					DataType.INTEGER,
					new Range<>(min, min, min),
					new Range<>(max + 1, max + 1, max + 1)
			);

			// Convert data for builder#prepare
			final String[] data = values.toArray(String[]::new);

			// Test if BUCKET_SIZE is suitable for data
			final long difference = max - min;
			if (difference < BUCKET_SIZE) {
				// If the difference is smaller than BUCKET_SIZE, we fall back to a flat hierarchy
				builder.addInterval(min, max);
				builder.prepare(data);
				return builder.build();
			}

			builder.setAggregateFunction(DataType.INTEGER.createAggregate().createIntervalFunction(true, false));

			// Define the interval for the first bucket on the lowest level
			builder.addInterval(min, min + BUCKET_SIZE);

			// Intervals of all other buckets are derived through the number of levels and groups in a level
			final int countLevels = Math.toIntExact(difference / BUCKET_SIZE);
			log.debug("Creating {} levels.", countLevels);
			for (int i = 0; i < countLevels; i++) {
				builder.getLevel(i).addGroup(GROUPS_PER_LEVEL);
			}
			// Add a final group that allows to differentiate between a present value ([min; max[) and NULL
			builder.getLevel(countLevels - 1).addGroup(1);

			// Preparation, so only hierarchy paths are created that are actually needed
			builder.prepare(data);

			return builder.build();
		}
	}


	@Slf4j
	class DecimalInterval implements AttributeTypeBuilder {
		private double min = Double.MAX_VALUE;
		private double max = Double.MIN_VALUE;

		private final Set<String> values = new HashSet<>();

		@Override
		public String register(Object value) {

			if (value == null) {
				values.add(DataType.NULL_VALUE);
				return DataType.NULL_VALUE;
			}

			if (!(value instanceof Number)) {
				throw new IllegalArgumentException("Expected a " + Number.class + " type, but got " + value.getClass() + ".");
			}

			final double l = ((Number) value).doubleValue();
			min = Math.min(min, l);
			max = Math.max(max, l);

			final String returnVal = Double.toString(l);
			values.add(returnVal);
			return returnVal;
		}

		@Override
		public AttributeType.Hierarchy build() {
			final HierarchyBuilderIntervalBased<Double> builder = HierarchyBuilderIntervalBased.create(
					DataType.DECIMAL,
					new Range<>(min, min, min),
					new Range<>(max + 1, max + 1, max + 1)
			);

			final String[] data = values.toArray(String[]::new);

			// Test if BUCKET_SIZE is suitable for data
			final double difference = max - min;
			if (difference < BUCKET_SIZE) {
				builder.addInterval(min, max);
				builder.prepare(data);
				return builder.build();
			}

			builder.setAggregateFunction(DataType.DECIMAL.createAggregate().createIntervalFunction(true, false));
			builder.addInterval(min, min + BUCKET_SIZE);

			final int countLevels = Math.toIntExact((long) (difference / BUCKET_SIZE));
			log.debug("Creating {} levels.", countLevels);
			for (int i = 0; i < countLevels; i++) {
				builder.getLevel(i).addGroup(GROUPS_PER_LEVEL);
			}
			builder.getLevel(countLevels - 1).addGroup(1);

			builder.prepare(data);

			return builder.build();
		}
	}

	class ConceptHierarchyNodeId implements AttributeTypeBuilder {

		@Getter
		private final TreeConcept concept;

		private final HashMap<String, ConceptTreeNode<?>> collectedIds;

		public ConceptHierarchyNodeId(TreeConcept concept) {
			this.concept = concept;
			this.collectedIds = new HashMap<>();
		}

		/**
		 * Check whether a result info is compatible to this {@link AttributeTypeBuilder}.
		 *
		 * @param info the {@link ResultInfo} that is checked
		 * @return return the referenced concept if compatible, else {@code null}
		 */
		@Nullable
		public static TreeConcept isCompatible(ResultInfo info) {
			if (!(info instanceof SelectResultInfo)) {
				return null;
			}

			SelectResultInfo selectResultInfo = (SelectResultInfo) info;
			final Select select = selectResultInfo.getSelect();
			if (!(select instanceof ConceptColumnSelect)) {
				return null;
			}

			if (!((ConceptColumnSelect) select).isAsIds()) {
				return null;
			}
			return (TreeConcept) select.getHolder().findConcept();
		}

		@Override
		public String register(Object value) {
			/*
			 * Workaround for the concept hierarchy generalization:
			 * Since Lists cannot be generalized at the moment,
			 * we take the first element into consideration if present.
			 */
			if (!(value instanceof List)) {
				throw new IllegalStateException("Expected a list to be returned from ConceptElementsAggregator, got " + value.getClass());
			}
			List<?> list = (List<?>) value;
			if (list.isEmpty()) {
				return "";
			}
			// Take the first element as the local id
			final int localId = (int) list.get(0);

			final ConceptTreeNode<?> node = concept.getElementByLocalId(localId);
			if (node == null) {
				// This should never be the case, as the local ids are our own stuff
				throw new NoSuchElementException();
			}
			final String id = extractGeneralizationLabel(node);
			collectedIds.put(id, node);
			return id;
		}

		private String extractGeneralizationLabel(ConceptTreeNode<?> node) {
			return node.getId().toStringWithoutDataset();
		}

		@Override
		public AttributeType build() {

			// Resolve ids and keep track of the maximum Depth
			final OptionalInt maxDepthOpt = collectedIds.values().stream().mapToInt(ConceptTreeNode::getDepth).max();

			if (maxDepthOpt.isEmpty()) {
				// There are no values to process: blank hierarchy
				return AttributeType.Hierarchy.create(new String[][]{{""}});
			}

			final int maxDepth = maxDepthOpt.getAsInt();

			// Build the hierarchy array (+2 because maxDepth starts at 0 and we need an extra level for "*")
			String[][] hierarchy = new String[collectedIds.size() + 1][maxDepth + 2];
			int insertElementWalk = 0;
			for (ConceptTreeNode<?> collectedElement : collectedIds.values()) {
				final int depth = collectedElement.getDepth();

				int insertDepthWalk = 0;
				while (insertDepthWalk <= (maxDepth - depth)) {
					// Fill up lower hierarchy levels with this same id as the element
					hierarchy[insertElementWalk][insertDepthWalk] = extractGeneralizationLabel(collectedElement);
					insertDepthWalk++;
				}

				ConceptTreeNode<?> parent = collectedElement.getParent();
				while (insertDepthWalk <= maxDepth) {
					// Fill up upper hierarchy levels with this parent ids
					if (parent == null) {
						throw new IllegalStateException(String.format(
								"Reached top level parent of concept %s, but depth was %d and max depths is %d. Current insertion is at %d.",
								concept.getId(), depth, maxDepth, insertDepthWalk
						));
					}

					hierarchy[insertElementWalk][insertDepthWalk] = extractGeneralizationLabel(parent);
					parent = parent.getParent();
					insertDepthWalk++;
				}
				hierarchy[insertElementWalk][maxDepth + 1] = SUPPRESSING_GRANULARITY;

				insertElementWalk++;
			}

			// Add hierarchy handling for empty values
			String[] emptyValueHierarchy = new String[maxDepth + 2];
			Arrays.fill(emptyValueHierarchy, "");
			emptyValueHierarchy[emptyValueHierarchy.length - 1] = SUPPRESSING_GRANULARITY;
			hierarchy[hierarchy.length - 1] = emptyValueHierarchy;

			return AttributeType.Hierarchy.create(hierarchy);
		}
	}
  
	/**
	 * Can be used in conjunction with {@link com.bakdata.conquery.models.types.SemanticType.IdentificationT}
	 *
	 * @implNote this might be only of use internal, because serialization might produce random objects.
	 * This can be useful for certain columns that are programmatically generated and then flagged with
	 * {@link AttributeType#INSENSITIVE_ATTRIBUTE} or {@link AttributeType#IDENTIFYING_ATTRIBUTE}.
	 * It can be also useful for select generated columns which want to provide their own {@link AttributeType.Hierarchy} and thereby becoming quasi-sensitive (see also {@link AttributeType#QUASI_IDENTIFYING_ATTRIBUTE}).
	 */
	@RequiredArgsConstructor
	class Fixed implements AttributeTypeBuilder {

		@NonNull
		private final AttributeType attributeType;

		@NonNull
		private final Function<Object, String> printer;

		@Override
		public String register(Object value) {
			return printer.apply(value);
		}

		@Override
		public AttributeType build() {
			return attributeType;
		}
	}
}
