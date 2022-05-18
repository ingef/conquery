package com.bakdata.conquery.apiv1.frontend;

import java.time.LocalDate;
import java.util.List;

import com.bakdata.conquery.apiv1.KeyValue;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.identifiable.ids.AId;
import lombok.Builder;
import lombok.Data;

/**
 * This class represents a concept as it is presented to the front end.
 */
@Data @Builder
public class FENode {
	private AId<?> parent;
	private String label;
	private String description;
	private Boolean active;
	private AId<?>[] children;
	private List<KeyValue> additionalInfos;
	private long matchingEntries;
	private Range<LocalDate> dateRange;
	private List<FETable> tables;
	private Boolean detailsAvailable;
	private boolean codeListResolvable;
	private List<FESelect> selects;
	private long matchingEntities;
}
