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
	org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttributeTypeBuilder.class);

	/**
	 * Fixed bucket sized for interval based hierarchies
	 */
	long BUCKET_SIZE = 5;

	/**
	 * Maximum number of buckets at the lowest level in the hierarchy for interval based hierarchies.
	 */
	int MAX_BUCKETS_LOWEST_LEVEL = 1 << 10;

	/**
	 * Number of groups within a bucket for interval based hierarchies
	 */
	int GROUPS_PER_LEVEL = 2;


	/**
	 * Register and transform a value that should be covered by the AttributeType (i.e. a {@link org.deidentifier.arx.AttributeType.Hierarchy}).
	 * <p>
	 * The return value is the string representation that was registered needs to be put into the {@link org.deidentifier.arx.Data}
	 * structure that is anonymized.
	 *
	 * @param value cell of the column that corresponds to this {@link AttributeTypeBuilder} of the result provided by conquery
	 * @return the registered value
	 */
	String register(Object value);

	/**
	 * Method to produce the AttributeType ({@link org.deidentifier.arx.AttributeType.Hierarchy}) after all data of a column was registered.
	 *
	 * @return the AttributeType for the corresponding column
	 */
	AttributeType build();

	/**
	 * Builds a two level {@link org.deidentifier.arx.AttributeType.Hierarchy}. Bottom level are the actual values. Top level is "*".
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
			if (value == null || Strings.isNullOrEmpty(value.toString())) {
				values.add(DataType.NULL_VALUE);
				return DataType.NULL_VALUE;
			}

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

			return AttributeType.Hierarchy.create(build.getHierarchy());
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

		/**
		 * Builds an interval based {@link org.deidentifier.arx.AttributeType.Hierarchy} for integer values.
		 * If the differnce between min and max of the registered data is larger than BUCKET_SIZE,
		 * the hierarchy will form a binary tree were the leaf nodes span over an interval of BUCKET_SIZE.
		 *
		 * @return the hierarchy
		 */
		@Override
		public AttributeType.Hierarchy build() {
			final long extendedMax = max + 1;
			final HierarchyBuilderIntervalBased<Long> builder = HierarchyBuilderIntervalBased.create(
					DataType.INTEGER,
					new Range<>(min, min, min),
					new Range<>(extendedMax, extendedMax, extendedMax)
			);

			// Convert data for builder#prepare
			final String[] data = values.toArray(String[]::new);

			log.debug("Creating a hierarchy for values in interval [{},{}]", min, max);

			builder.setAggregateFunction(DataType.INTEGER.createAggregate().createIntervalFunction(true, false));

			// Test if BUCKET_SIZE is suitable for data
			final long difference = max - min;
			if (difference < BUCKET_SIZE) {
				// If the difference is smaller than BUCKET_SIZE, we fall back to a flat hierarchy
				builder.addInterval(min, extendedMax);
				builder.prepare(data);
				return builder.build();
			}

			// Calculate how many buckets are necessary with the default bucket size
			final int countDefaultSizeLeafBuckets = Math.toIntExact((long) Math.ceil((float) difference / BUCKET_SIZE));

			// Calculate actual bucket size to stay within maximum number of buckets
			long bucketSize = countDefaultSizeLeafBuckets > MAX_BUCKETS_LOWEST_LEVEL ?
							  Math.toIntExact((long) Math.ceil((float) (difference / MAX_BUCKETS_LOWEST_LEVEL))) :
							  BUCKET_SIZE;


			final int countLeafBuckets = Math.min(countDefaultSizeLeafBuckets, MAX_BUCKETS_LOWEST_LEVEL);

			builder.addInterval(min, min + bucketSize);

			log.info("Creating hierarchy with bucket size: {}", bucketSize);

			prepareBuckets(builder, countLeafBuckets);


			builder.prepare(data);

			return builder.build();
		}
	}


	@Slf4j
	class DecimalInterval implements AttributeTypeBuilder {
		/**
		 * We add a small epsilon to the max-limit, so that the actual max is included in the interval.
		 */
		public static final double EPSILON = 10e-4;
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
			min = Math.min(Math.floor(min), l);
			max = Math.max(Math.ceil(max), l);

			final String returnVal = Double.toString(l);
			values.add(returnVal);
			return returnVal;
		}

		@Override
		public AttributeType.Hierarchy build() {

			final double extendedMax = max + EPSILON;
			final HierarchyBuilderIntervalBased<Double> builder = HierarchyBuilderIntervalBased.create(
					DataType.DECIMAL,
					new Range<>(min, min, min),
					new Range<>(max, extendedMax, extendedMax)
			);

			final String[] data = values.toArray(String[]::new);

			log.debug("Creating a hierarchy for values in interval [{},{}]", min, max);

			final double difference = max - min;

			builder.setAggregateFunction(DataType.DECIMAL.createAggregate().createIntervalFunction(true, false));

			if (difference < BUCKET_SIZE) {
				builder.addInterval(min, max);
				builder.prepare(data);
				return builder.build();
			}


			// Calculate how many buckets are necessary with the default bucket size
			final int countDefaultSizeLeafBuckets = Math.toIntExact((long) Math.ceil(difference / BUCKET_SIZE));

			// Calculate actual bucket size to stay within maximum number of buckets
			double bucketSize = countDefaultSizeLeafBuckets > MAX_BUCKETS_LOWEST_LEVEL ?
								(difference / MAX_BUCKETS_LOWEST_LEVEL) :
								BUCKET_SIZE;


			final int countLeafBuckets = Math.min(countDefaultSizeLeafBuckets, MAX_BUCKETS_LOWEST_LEVEL);

			builder.addInterval(min, min + bucketSize);

			log.info("Creating hierarchy with bucket size: {}", bucketSize);

			prepareBuckets(builder, countLeafBuckets);

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
				hierarchy[insertElementWalk][maxDepth + 1] = DataType.ANY_VALUE;

				insertElementWalk++;
			}

			// Add hierarchy handling for empty values
			String[] emptyValueHierarchy = new String[maxDepth + 2];
			Arrays.fill(emptyValueHierarchy, DataType.NULL_VALUE);
			emptyValueHierarchy[emptyValueHierarchy.length - 1] = DataType.ANY_VALUE;
			hierarchy[hierarchy.length - 1] = emptyValueHierarchy;

			return AttributeType.Hierarchy.create(hierarchy);
		}
	}
  
	/**
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

	/**
	 * Calculates the minimal number of levels for a binary-tree given assuming that
	 * all leaf nodes are on the same level and there is at maximum one path (root -...-> leaf)
	 * through the tree, where parent nodes only have one child.
	 * <br/>
	 * <p>
	 * Example:
	 * Given a leaf level with 200 nodes, this will be the nodes per level from leaf to root:
	 * <ul>
	 * <li>200 -> leaf level</li>
	 * <li>100</li>
	 * <li>50</li>
	 * <li>25</li>
	 * <li>13</li>
	 * <li>7</li>
	 * <li>4</li>
	 * <li>2</li>
	 * <li>1 -> root level</li>
	 * </ul>
	 * Which results in 9 levels.
	 * <p>
	 * The formula is then: ceil(log<sub>2</sub>(x)) + 1
	 * </p>
	 *
	 * @param countLeafNodes
	 * @return
	 */
	static int countLevelsFromLeafNodes(int countLeafNodes) {
		if (countLeafNodes <= 0) {
			throw new IllegalArgumentException();
		}
		return 32 - Integer.numberOfLeadingZeros(countLeafNodes - 1) + 1;
	}


	/**
	 * Adds buckets to the hierarchy. At most two buckets merge into a parent node forming a binary tree.
	 * The binary tree property relies on {@link AttributeTypeBuilder#countLevelsFromLeafNodes} and {@link AttributeTypeBuilder#GROUPS_PER_LEVEL} being set 2.
	 */
	static <T> void prepareBuckets(HierarchyBuilderIntervalBased<T> builder, int countLeafBuckets) {
		// We subtract one level, because the top-level-bucket (min,max) is already defined
		final int countLevels = countLevelsFromLeafNodes(countLeafBuckets) - 1;
		log.info("Creating {} levels.", countLevels);
		for (int i = 0; i < countLevels; i++) {
			builder.getLevel(i).addGroup(GROUPS_PER_LEVEL);
		}

		builder.getLevel(countLevels - 1).addGroup(1);
	}
}
