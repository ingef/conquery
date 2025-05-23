package com.bakdata.conquery.apiv1.forms.export_form;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import c10n.C10N;
import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.InternalForm;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.CQYes;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.apiv1.query.concept.specific.CQNegation;
import com.bakdata.conquery.internationalization.ExportFormC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.forms.util.ResolutionShortNames;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@CPSType(id = "EXPORT_FORM", base = QueryDescription.class)
@EqualsAndHashCode(callSuper = true)
@ToString
public class ExportForm extends Form implements InternalForm {

	@Getter
	@Setter
	@EqualsAndHashCode.Exclude
	private JsonNode values;


	@Nullable
	@JsonProperty("queryGroup")
	private ManagedExecutionId queryGroupId;

	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private ManagedQuery queryGroup;

	@NotNull
	@Valid
	@JsonManagedReference
	private Mode timeMode;

	@NotEmpty
	@Valid
	private List<CQElement> features = ImmutableList.of();

	@NotNull
	@NotEmpty
	private List<ResolutionShortNames> resolution = List.of(ResolutionShortNames.COMPLETE);

	private boolean alsoCreateCoarserSubdivisions = false;

	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private Query prerequisite;
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private List<Resolution> resolvedResolutions;

	/**
	 * Maps the given resolution to a fitting alignment. It tries to use the alignment which was given as a hint.
	 * If the alignment does not fit to a resolution (resolution is finer than the alignment), the first alignment that
	 * this resolution supports is chosen (see the alignment order in {@link Resolution})
	 * @param resolutions The temporal resolutions for which sub queries should be generated per entity
	 * @param alignmentHint The preferred calendar alignment on which the sub queries of each resolution should be aligned.
	 * 						Note that this alignment is chosen when a resolution is equal or coarser.
	 * @return The given resolutions mapped to a fitting calendar alignment.
	 */
	public static List<ResolutionAndAlignment> getResolutionAlignmentMap(List<Resolution> resolutions, Alignment alignmentHint) {

		return resolutions.stream()
				.map(r -> ResolutionAndAlignment.of(r, getFittingAlignment(alignmentHint, r)))
				.collect(Collectors.toList());
	}

	private static Alignment getFittingAlignment(Alignment alignmentHint, Resolution resolution) {
		if(resolution.isAlignmentSupported(alignmentHint) ) {
			return alignmentHint;
		}
		return resolution.getDefaultAlignment();
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		timeMode.visit(visitor);
		features.forEach(visitor);
	}

	@Override
	public Map<String, Query> createSubQueries() {
		return Map.of(
				ConqueryConstants.SINGLE_RESULT_TABLE_NAME,
				timeMode.createSpecializedQuery()
		);
	}

	@Override
	public String getLocalizedTypeLabel() {
		return C10N.get(ExportFormC10n.class, I18n.LOCALE.get()).getType();
	}

	@Override
	public ManagedInternalForm<ExportForm> toManagedExecution(UserId user, DatasetId submittedDataset, MetaStorage storage, DatasetRegistry<?> datasetRegistry,
															  ConqueryConfig config) {
		return new ManagedInternalForm<>(this, user, submittedDataset, storage, datasetRegistry, config);
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		if (queryGroupId == null) {
			return Collections.emptySet();
		}

		return Set.of(queryGroupId);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		if(queryGroupId != null) {
			queryGroup = (ManagedQuery) queryGroupId.resolve();
			prerequisite = queryGroup.getQuery();
		}
		else {
			prerequisite = new ConceptQuery(new CQYes());
		}


		// Apply defaults to user concept
		DefaultSelectSettable.enable(features);

		timeMode.resolve(context);

		List<Resolution> resolutionsFlat = resolution.stream()
													 .flatMap(ResolutionShortNames::correspondingResolutions)
													 .distinct()
													 .toList();


		if (isAlsoCreateCoarserSubdivisions()) {
			if (resolutionsFlat.size() != 1) {
				throw new IllegalStateException("Abort Form creation, because coarser subdivision are requested and multiple resolutions are given. With 'alsoCreateCoarserSubdivisions' set to true, provide only one resolution.");
			}
			resolvedResolutions = resolutionsFlat.getFirst().getThisAndCoarserSubdivisions();
		}
		else {
			resolvedResolutions = resolutionsFlat;
		}
	}

	/**
	 * Classes that can be used as Features in ExportForm, having default-exists, are triggered this way.
	 */
	public interface DefaultSelectSettable {

		static void enable(CQElement feature) {
			switch (feature) {
				case DefaultSelectSettable settable  -> settable.setDefaultSelects();
				// CQNegation and CQDateRestriction chain CQElements and don't have selects themselves
				case CQNegation negation -> enable(negation.getChild());
				case CQDateRestriction dr -> enable(dr.getChild());
				default -> {}
			}
		}

		static void enable(List<CQElement> features) {
			for (CQElement feature : features) {
				enable(feature);
			}
		}

		void setDefaultSelects();
	}

	/**
	 * Serializable helper container to combine a resolution and an alignment.
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@Getter
	public static class ResolutionAndAlignment {
		private final Resolution resolution;
		private final Alignment alignment;

		@JsonCreator
		public static ResolutionAndAlignment of(Resolution resolution, Alignment alignment){
			if (!resolution.isAlignmentSupported(alignment)) {
				throw new ValidationException(String.format("The alignment %s is not supported by the resolution %s", alignment, resolution));
			}

			return new ResolutionAndAlignment(resolution, alignment);
		}
	}
}
