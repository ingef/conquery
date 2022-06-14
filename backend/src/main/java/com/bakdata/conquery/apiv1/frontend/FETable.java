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
	private List<@Valid FEFilter> filters;
	private List<@Valid FESelect> selects;
	@JsonProperty("default")
	private Boolean isDefault;
	private Set<SecondaryIdDescriptionId> supportedSecondaryIds;
}
