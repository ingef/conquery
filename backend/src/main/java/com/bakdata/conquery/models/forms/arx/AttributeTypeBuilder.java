package com.bakdata.conquery.models.forms.arx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.DataType;
import org.deidentifier.arx.aggregates.HierarchyBuilderDate;

/**
 * Helper classes that allow us to gather values for possible attribute hierarchies, while
 * we convert from the internal conquery result format to the ARX {@link org.deidentifier.arx.Data} format.
 */
public interface AttributeTypeBuilder {

	public static final String SUPPRESSING_GRANULARITY = "*";


	String register(String value);

	AttributeType build();

	/**
	 * Builds a two level hierarchy. Bottom level are the actual values. Top level is "*".
	 */
	class Flat implements AttributeTypeBuilder {

		private final Set<String> values = new HashSet<>();

		@Override
		public String register(String value) {
			values.add(value);
			return value;
		}

		@Override
		public AttributeType build() {
			return AttributeType.Hierarchy.create(values.stream().map(v -> new String[]{v, "*"}).toArray(String[][]::new));
		}
	}

	class Date implements AttributeTypeBuilder {
		private final Set<String> values = new HashSet<>();

		@Override
		public String register(String value) {
			values.add(value);
			return value;
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
			String[][] newhierarchy = Arrays.copyOf(hierarchy, hierarchy.length + 1);
			String[] emptyValueHierarchy = new String[hierarchy.length > 0 ? hierarchy[0].length : 0];
			Arrays.fill(emptyValueHierarchy, "");
			emptyValueHierarchy[emptyValueHierarchy.length - 1] = SUPPRESSING_GRANULARITY;
			newhierarchy[newhierarchy.length - 1] = emptyValueHierarchy;

			return AttributeType.Hierarchy.create(newhierarchy);
		}
	}

	class ConceptHierarchyNodeId implements AttributeTypeBuilder {

		@Getter
		private final TreeConcept concept;


		private final HashMap<String, ConceptTreeNode<?>> collectedIds;

		public ConceptHierarchyNodeId(TreeConcept concept) {
			this.concept = concept;
			this.collectedIds = new HashMap<>((int) concept.getAllChildren().count());
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
		public String register(String value) {
			final int localId = Integer.parseInt(value);
			final ConceptTreeNode<?> node = concept.getElementByLocalId(localId);
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
			final Optional<Integer> maxDepthOpt = collectedIds.values().stream().map(ConceptTreeNode::getDepth).max(Integer::compareTo);

			if (maxDepthOpt.isEmpty()) {
				// There are no values to process: blank hierarchy
				return AttributeType.Hierarchy.create(new String[][]{{""}});
			}

			final int maxDepth = maxDepthOpt.get();

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

		@Override
		public String register(String value) {
			return value;
		}

		@Override
		public AttributeType build() {
			return attributeType;
		}
	}
}
