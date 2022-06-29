package com.bakdata.conquery.models.types;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public abstract class SemanticType {
	/**
	 * Column containing primary Event dates. There should only ever be one EVENT_DATE per Query.
	 */
	@CPSType(id = "EVENT_DATE", base = SemanticType.class)
	@Data
	public static class EventDateT extends SemanticType {

	}

	/**
	 * Column contains the source of the result line.
	 * <p>
	 * At the moment, only used in {@link com.bakdata.conquery.apiv1.query.TableExportQuery}.
	 */
	@CPSType(id = "SOURCES", base = SemanticType.class)
	@Data
	public static class SourcesT extends SemanticType {

	}

	/**
	 * Column contains a fixed set of String values.
	 */
	@CPSType(id = "CATEGORICAL", base = SemanticType.class)
	@Data
	public static class CategoricalT extends SemanticType {
		//TODO maybe try and embed the universe?
	}

	/**
	 * Column contains {@link com.bakdata.conquery.models.forms.util.Resolution} from an {@link com.bakdata.conquery.apiv1.forms.export_form.ExportForm}.
	 */
	@CPSType(id = "RESOLUTION", base = SemanticType.class)
	@Data
	public static class ResolutionT extends SemanticType {

	}

	/**
	 * Column contains an Entity's Id of a kind.
	 * <p>
	 * See {@link com.bakdata.conquery.models.config.ColumnConfig} / {@link com.bakdata.conquery.models.config.FrontendConfig.UploadConfig}for the source of this.
	 */
	@CPSType(id = "ID", base = SemanticType.class)
	@Data
	@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
	public static class IdT extends SemanticType {
		private final String kind;
	}

	/**
	 * Column contains values of a {@link SecondaryIdDescription}.
	 */
	@CPSType(id = "SECONDARY_ID", base = SemanticType.class)
	@Data
	@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
	public static class SecondaryIdT extends SemanticType {
		@NsIdRef
		private final SecondaryIdDescription secondaryId;
	}

	/**
	 * Column contains the results of a {@link Select}.
	 */
	@CPSType(id = "SELECT", base = SemanticType.class)
	@Data
	@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
	public static class SelectResultT extends SemanticType {
		@NsIdRef
		private final Select select;
	}

	/**
	 * Column contains values used by a Connector of a Concept.
	 *
	 * Only used for {@link com.bakdata.conquery.apiv1.query.TableExportQuery}.
	 */
	@CPSType(id = "CONCEPT_COLUMN", base = SemanticType.class)
	@Data
	@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
	public static class ConceptColumnT extends SemanticType {
		@NsIdRef
		private final Concept<?> concept;

	}
}
