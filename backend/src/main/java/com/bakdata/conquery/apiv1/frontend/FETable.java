package com.bakdata.conquery.apiv1.frontend;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * This class represents a concept filter parameter as it is presented to the front end.
 */
@Data
@Builder
public class FETable {
	@NotNull
	private TableId id;

	private ConnectorId connectorId;

	@NotEmpty
	private String label;

	private FEValidityDate dateColumn;

	//TODO MT: This is a weird syntax but hey: https://youtrack.jetbrains.com/issue/IDEA-241656/Provide-a-fix-for-static-member-qualifying-type-may-not-be-annotated#focus=Comments-27-4158847.0-0
	private List<FEFilterConfiguration.@Valid Top> filters;

	private List<@Valid FESelect> selects;

	@JsonProperty("default")
	private Boolean isDefault;

	private Set<SecondaryIdDescriptionId> supportedSecondaryIds;
}
