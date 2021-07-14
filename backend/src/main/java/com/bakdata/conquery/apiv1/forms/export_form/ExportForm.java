package com.bakdata.conquery.apiv1.forms.export_form;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import c10n.C10N;
import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.internationalization.ExportFormC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
@CPSType(id="EXPORT_FORM", base=QueryDescription.class)
public class ExportForm extends Form {

	@NotNull
	@JsonProperty("queryGroup")
	private ManagedExecutionId queryGroupId;

	@JsonIgnore
	private ManagedQuery queryGroup;

	@NotNull @Valid @JsonManagedReference
	private Mode timeMode;
	
	@NotNull @NotEmpty
	private List<DateContext.Resolution> resolution = List.of(DateContext.Resolution.COMPLETE);
	
	private boolean alsoCreateCoarserSubdivisions = true;

	@JsonIgnore
	private Query prerequisite;
	@JsonIgnore
	private List<DateContext.Resolution> resolvedResolutions;

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		timeMode.visit(visitor);
	}


	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, User user, Dataset submittedDataset) {
		return Map.of(
			ConqueryConstants.SINGLE_RESULT_TABLE_NAME,
			List.of(
				timeMode.createSpecializedQuery(datasets, user, submittedDataset)
					.toManagedExecution(user, submittedDataset)));
	}

	@Override
	public Set<ManagedExecution<?>> collectRequiredQueries() {
		if(queryGroup == null){
			return Collections.emptySet();
		}

		return Set.of(queryGroup);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		queryGroup = (ManagedQuery) context.getDatasetRegistry().getMetaRegistry().resolve(queryGroupId);

		timeMode.resolve(context);
		prerequisite = queryGroup.getQuery();


		if(isAlsoCreateCoarserSubdivisions()) {
			if(getResolution().size() != 1) {
				throw new IllegalStateException("Abort Form creation, because coarser subdivision are requested and multiple resolutions are given. With 'alsoCreateCoarserSubdivisions' set to true, provide only one resolution.");
			}
			resolvedResolutions = getResolution().get(0).getThisAndCoarserSubdivisions();
		}
		else {
			resolvedResolutions = getResolution();
		}
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

	/**
	 * Classes that can be used as Features in ExportForm, having default-exists, are triggered this way.
	 */
	public static interface DefaultSelectSettable {
		public static void enable(List<CQElement> features) {
			for (CQElement feature : features) {
				if(feature instanceof DefaultSelectSettable){
					((DefaultSelectSettable) feature).setDefaultExists();
				}
			}
		}

		void setDefaultExists();
	}


	@Override
	public ManagedForm toManagedExecution(User user, Dataset submittedDataset) {
		return new ManagedInternalForm(this, user, submittedDataset);
	}
}
