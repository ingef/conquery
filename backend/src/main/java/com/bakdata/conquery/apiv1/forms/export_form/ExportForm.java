package com.bakdata.conquery.apiv1.forms.export_form;

import c10n.C10N;
import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.internationalization.ExportFormC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.NamespacedIdHolding;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Getter @Setter
@CPSType(id="EXPORT_FORM", base=QueryDescription.class)
public class ExportForm implements Form, NamespacedIdHolding {
	@NotNull
	private ManagedExecutionId queryGroup;
	@NotNull @Valid @JsonManagedReference
	private Mode timeMode;
	
	@NotNull @NotEmpty
	private List<DateContext.Resolution> resolution = List.of(DateContext.Resolution.COMPLETE);
	
	private boolean alsoCreateCoarserSubdivisions = true;

	@JsonIgnore
	private IQuery prerequisite;

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		timeMode.visit(visitor);
	}

	@Override
	public void collectNamespacedIds(Set<NamespacedId> ids) {
		checkNotNull(ids);
		if(queryGroup != null) {
			ids.add(queryGroup);
		}
	}

	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, UserId userId, DatasetId submittedDataset) {
		return Map.of(
			ConqueryConstants.SINGLE_RESULT_TABLE_NAME,
			List.of(
				timeMode.createSpecializedQuery(datasets, userId, submittedDataset)
					.toManagedExecution(datasets, userId, submittedDataset)));
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Set.of(queryGroup);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		timeMode.resolve(context);
		prerequisite = Form.resolvePrerequisite(context, queryGroup);
	}

	@Override
	public String getLocalizedTypeLabel() {
		return C10N.get(ExportFormC10n.class, I18n.LOCALE.get()).getType();
	}


	/**
	 * Maps the given resolution to a fitting alignment. It tries to use the alignment which was given as a hint.
	 * If the alignment does not fit to a resolution (resolution is finer than the alignment), the first alignment that
	 * this resolution supports is chosen (see the alignment order in {@link DateContext.Resolution})
	 * @param resolutions The temporal resolutions for which sub queries should be generated per entity
	 * @param alignmentHint The preferred calendar alignment on which the sub queries of each resolution should be aligned.
	 * 						Note that this alignment is chosen when a resolution is equal or coarser.
	 * @return The given resolutions mapped to a fitting calendar alignment.
	 */
	public static List<ExportForm.ResolutionAndAlignment> getResolutionAlignmentMap(List<DateContext.Resolution> resolutions, DateContext.Alignment alignmentHint) {

		return resolutions.stream()
				.map(r -> ResolutionAndAlignment.of(r, getFittingAlignment(alignmentHint, r)))
				.collect(Collectors.toList());
	}

	private static DateContext.Alignment getFittingAlignment(DateContext.Alignment alignmentHint, DateContext.Resolution resolution) {
		return resolution.getSupportedAlignments().contains(alignmentHint)? alignmentHint : resolution.getSupportedAlignments().iterator().next();
	}

	/**
	 * Serializable helper container to combine a resolution and an alignment.
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@Getter
	public static class ResolutionAndAlignment {
		private final  DateContext.Resolution resolution;
		private final DateContext.Alignment alignment;

		@JsonCreator
		public static ResolutionAndAlignment of(DateContext.Resolution resolution, DateContext.Alignment alignment){
			if (!resolution.getSupportedAlignments().contains(alignment)) {
				throw new ValidationException(String.format("The alignment %s is not supported by the resolution %s", alignment, resolution));
			}

			return new ResolutionAndAlignment(resolution, alignment);
		}
	}
}
