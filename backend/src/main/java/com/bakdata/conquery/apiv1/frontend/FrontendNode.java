package com.bakdata.conquery.apiv1.frontend;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.KeyValue;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.Builder;
import lombok.Data;

/**
 * This class represents a concept as it is presented to the front end.
 */
@Data
@Builder
public class FrontendNode {
	private NamespacedId<?> parent;
	@NotNull
	private String label;
	private String description;
	private Boolean active;
	private NamespacedId<?>[] children;
	private List<KeyValue> additionalInfos;
	private long matchingEntries;
	private Range<LocalDate> dateRange;
	private List<@Valid FrontendTable> tables;
	private Boolean detailsAvailable;
	private boolean codeListResolvable;
	private List<FrontendSelect> selects;
	private long matchingEntities;
	private boolean excludeFromTimeAggregation;
}
