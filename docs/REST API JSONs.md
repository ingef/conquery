
# REST API JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.


# REST endpoints

### GET /datasets<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/DatasetsResource.java#L26)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.DatasetsResource`

Method: `getDatasets`

Returns: list of [IdLabel](#Type-IdLabel)

</p></details>

### GET config/frontend<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConfigResource.java#L23)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConfigResource`

Method: `getFrontendConfig`

Returns: [FrontendConfig](#Type-FrontendConfig)

</p></details>

### GET datasets/{dataset}/concepts<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/DatasetResource.java#L28)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.DatasetResource`

Method: `getRoot`

Returns: [FERoot](#Type-FERoot)

</p></details>

### GET datasets/{dataset}/concepts/{concept}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L42)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource`

Method: `getNode`

Returns: `Response`

</p></details>

### POST datasets/{dataset}/concepts/{concept}/resolve<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L55)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource`

Method: `resolve`

Expects: [ConceptCodeList](#Type-ConceptCodeList)
Returns: [ResolvedConceptsResult](#Type-ResolvedConceptsResult)

</p></details>

### POST datasets/{dataset}/concepts/{concept}/tables/{table}/filters/{filter}/autocomplete<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L44)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource`

Method: `autocompleteTextFilter`

Expects: [AutocompleteRequest](#Type-AutocompleteRequest)
Returns: `AutoCompleteResult`

</p></details>

### POST datasets/{dataset}/concepts/{concept}/tables/{table}/filters/{filter}/resolve<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L38)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource`

Method: `resolveFilterValues`

Expects: [FilterValues](#Type-FilterValues)
Returns: [ResolvedConceptsResult](#Type-ResolvedConceptsResult)

</p></details>

### GET datasets/{dataset}/entity-preview<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/DatasetResource.java#L34-L36)</sup></sub></sup>
Provides list of default {@link ConnectorId}s to use for {@link QueryResource.EntityPreview#getSources()}.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.DatasetResource`

Method: `getEntityPreviewDefaultConnectors`

Returns: `Stream<ConnectorId>`

</p></details>

### GET datasets/{dataset}/queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L69)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `getAllQueries`

Expects: `Optional<Boolean>`
Returns: list of [ExecutionStatus](#Type-ExecutionStatus)

</p></details>

### POST datasets/{dataset}/queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L78)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `postQuery`

Expects: `Optional<Boolean>`
Expects: [@NotNull(message="{javax.validation.constraints.NotNull.message}", groups={}, payload={}) @Valid QueryDescription](#Base-QueryDescription)
Returns: `Response`

</p></details>

### POST datasets/{dataset}/queries/entity<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L160)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `getEntityData`

Expects: `EntityPreview`
Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### POST datasets/{dataset}/queries/upload<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L144)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `upload`

Expects: `@Valid ExternalUpload`
Returns: `ExternalUploadResult`

</p></details>

### GET datasets/{dataset}/queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L90)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `getStatus`

Expects: `Optional<Boolean>`
Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### PATCH datasets/{dataset}/queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L103)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `patchQuery`

Expects: `Optional<Boolean>`
Expects: [MetaDataPatch](#Type-MetaDataPatch)
Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### DELETE datasets/{dataset}/queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L115)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `deleteQuery`

Returns: `void`

</p></details>

### POST datasets/{dataset}/queries/{query}/cancel<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L134)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `cancel`

Returns: `void`

</p></details>

### POST datasets/{dataset}/queries/{query}/reexecute<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L124)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `reexecute`

Expects: `Optional<Boolean>`
Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### GET datasets/{dataset}/result/{query}.csv<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ResultCsvResource.java#L53)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ResultCsvResource`

Method: `getAsCsv`

Expects: `String`
Expects: `String`
Expects: `Optional<Boolean>`
Returns: `Response`

</p></details>

---

## Base QueryDescription


Different types of QueryDescription can be used by setting `type` to one of the following values:


### ABSOLUTE_FORM_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L32)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L39) | dateRange | `@NotNull @Valid Range<LocalDate>` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L41) | features | [ARRAY_CONCEPT_QUERY](#ARRAY_CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L37) | query | `@NotNull @Valid Query` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L43) | resolutionsAndAlignmentMap | list of `ResolutionAndAlignment` | ? |  |  | 
</p></details>

### ARRAY_CONCEPT_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L33-L37)</sup></sub></sup>
Query type that combines a set of {@link ConceptQuery}s which are separately evaluated and whose results are merged. If a SpecialDateUnion is required, the result will hold the union of all dates from the separate queries.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.ArrayConceptQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L45) | childQueries | list of [CONCEPT_QUERY](#CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L48) | dateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L52) | resolvedDateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
</p></details>

### ARX_FORM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/ArxForm.java#L33-L37)</sup></sub></sup>
Form that performs an anonymization using the ARX library on the result of the given execution.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.forms.ArxForm`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/ArxForm.java#L80-L82) | historySize | `@javax.validation.constraints.Min(1) int` | `200` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/ArxForm.java#L47-L49) | kAnonymityParam | `@javax.validation.constraints.Min(2) int` | `2` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/ArxForm.java#L64-L66) | maximumSnapshotSizeDataset | `@javax.validation.constraints.DecimalMax("1") @javax.validation.constraints.DecimalMin(value="0", inclusive=false) double` | `0.2` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/ArxForm.java#L72-L74) | maximumSnapshotSizeSnapshot | `@javax.validation.constraints.DecimalMax("1") @javax.validation.constraints.DecimalMin(value="0", inclusive=false) double` | `0.2` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/ArxForm.java#L43) | queryGroupId | ID of `ManagedExecution` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/ArxForm.java#L55-L57) | suppressionLimit | `@javax.validation.constraints.DecimalMax("1") @javax.validation.constraints.DecimalMin("0") double` | `0.02` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/Form.java#L32-L35) | values | `JsonNode` | `null` |  | Raw form config (basically the raw format of this form), that is used by the backend at the moment to create a {@link com.bakdata.conquery.models.forms.configs.FormConfig} upon start of this form (see {@link ManagedForm#start()}). | 
</p></details>

### CONCEPT_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L31)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.ConceptQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L42) | dateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L46) | resolvedDateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L38) | root | [@Valid @NotNull CQElement](#Base-CQElement) | ? |  |  | 
</p></details>

### ENTITY_DATE_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/EntityDateQuery.java#L31-L35)</sup></sub></sup>
This query uses the date range defined by {@link EntityDateQuery#query} for each entity and applies it as a date restriction for the following query defined by {@link EntityDateQuery#features}. The additional {@link EntityDateQuery#dateRange} is applied globally on all entities.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.EntityDateQuery`

No fields can be set for this type.

</p></details>

### EXPORT_FORM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L45)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.forms.export_form.ExportForm`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/Form.java#L32-L35) | values | `JsonNode` | `null` |  | Raw form config (basically the raw format of this form), that is used by the backend at the moment to create a {@link com.bakdata.conquery.models.forms.configs.FormConfig} upon start of this form (see {@link ManagedForm#start()}). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L62) | alsoCreateCoarserSubdivisions | `boolean` | `true` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L49) | queryGroupId | ID of `ManagedExecution` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L59) | resolution | list of one of COMPLETE, YEARS, QUARTERS, DAYS | `["COMPLETE"]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L56) | timeMode | `@NotNull @Valid Mode` | `null` |  |  | 
</p></details>

### FULL_EXPORT_FORM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L41)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.forms.export_form.FullExportForm`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/Form.java#L32-L35) | values | `JsonNode` | `null` |  | Raw form config (basically the raw format of this form), that is used by the backend at the moment to create a {@link com.bakdata.conquery.models.forms.configs.FormConfig} upon start of this form (see {@link ManagedForm#start()}). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L53) | dateRange | `@Valid Range<LocalDate>` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L46) | queryGroupId | ID of `ManagedExecution` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L57) | tables | list of [CONCEPT](#CONCEPT) | `[]` |  |  | 
</p></details>

### RELATIVE_FORM_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L33)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.RelativeFormQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L39) | features | [ARRAY_CONCEPT_QUERY](#ARRAY_CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L43) | indexPlacement | one of BEFORE, NEUTRAL, AFTER | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L41) | indexSelector | one of EARLIEST, LATEST, RANDOM | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L37) | query | `@NotNull @Valid Query` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L51) | resolutionsAndAlignmentMap | list of `ResolutionAndAlignment` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L47) | timeCountAfter | `@javax.validation.constraints.Min(0) int` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L45) | timeCountBefore | `@javax.validation.constraints.Min(0) int` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L49) | timeUnit | one of DAYS, QUARTERS, YEARS | ? |  |  | 
</p></details>

### SECONDARY_ID_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L40)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.SecondaryIdQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L53) | dateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | `"MERGE"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L57-L59) | query | [CONCEPT_QUERY](#CONCEPT_QUERY) | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L46) | root | [@NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L49) | secondaryId | ID of `@NsIdRef @NotNull SecondaryIdDescription` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L63) | withSecondaryId | list of ID of `@NsIdRefCollection Set<Column>` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L67) | withoutSecondaryId | list of ID of `@NsIdRefCollection Set<Table>` | ␀ |  |  | 
</p></details>

### TABLE_EXPORT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L56-L66)</sup></sub></sup>
TableExportQuery can be used to export raw data from selected {@link Connector}s, for selected {@link com.bakdata.conquery.models.query.entity.Entity}s. <p> Output format is lightly structured: 1: Contains the {@link com.bakdata.conquery.models.datasets.concepts.ValidityDate} if one is available for the event. 2: Contains the source {@link com.bakdata.conquery.models.datasets.Table}s label. 3 - X: Contain the SecondaryId columns de-duplicated. Following: Columns of all tables, (except for SecondaryId Columns), grouped by tables. The order is not guaranteed. <p> Columns used in Connectors to build Concepts, are marked with {@link SemanticType.ConceptColumnT} in {@link FullExecutionStatus#getColumnDescriptions()}.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.TableExportQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L78) | dateRange | `@NotNull Range<LocalDate>` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L85-L91) | positions | map from `Column` to `int` or `null` | ? |  | We collect the positions for each Column of the output in here. Multiple columns can map to the same output position: - ValidityDate-Columns are merged into a single Date-Column - SecondaryIds are collected into a Column per SecondaryId - The remaining columns are arbitrarily ordered, but usually grouped by their source table. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L74) | query | `@NonNull Query` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L81) | tables | list of [CONCEPT](#CONCEPT) | ? |  |  | 
</p></details>



---

## Base CQElement


Different types of CQElement can be used by setting `type` to one of the following values:


### AND<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L39)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQAnd`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L42) | children | list of [CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L48) | createExists | `Optional<Boolean>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L51) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQBeforeTemporalQuery.java#L8-L10)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQBeforeTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### BEFORE_OR_SAME<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQBeforeOrSameTemporalQuery.java#L8-L10)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened on the same day or before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQBeforeOrSameTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### CONCEPT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L55)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQConcept`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L86) | aggregateEventDates | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L62-L64) | elements | list of ID of `ConceptElement<?>` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L83) | excludeFromSecondaryId | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L80) | excludeFromTimeAggregation | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L76) | selects | list of ID of `Select` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L70) | tables | list of [CQTable](#Type-CQTable) | `[]` |  |  | 
</p></details>

### DATE_RESTRICTION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQDateRestriction.java#L31)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### DAYS_BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQDaysBeforeTemporalQuery.java#L10-L12)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQDaysBeforeTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### DAYS_OR_NO_EVENT_BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQDaysBeforeOrNeverTemporalQuery.java#L9-L11)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}, or no events. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQDaysBeforeOrNeverTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### EXTERNAL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/external/CQExternal.java#L48-L50)</sup></sub></sup>
Allows uploading lists of entities.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
</p></details>

### NEGATION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQNegation.java#L25)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQNegation`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQNegation.java#L30) | child | [@Valid @NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQNegation.java#L33) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### OR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L41)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQOr`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L45) | children | list of [CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L51) | createExists | `Optional<Boolean>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L55) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### RESULT_INFO_DECORATOR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/ResultInfoDecorator.java#L28-L30)</sup></sub></sup>
A wrapper for {@link CQElement}s to provide additional infos to parts of a query.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.ResultInfoDecorator`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/ResultInfoDecorator.java#L41) | child | [@NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/ResultInfoDecorator.java#L39) | values | ClassToInstanceMap maps from base class `Object` to instances of subtypes |  |  |  | 
</p></details>

### SAME<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQSameTemporalQuery.java#L8-L10)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} at the same time as the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQSameTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### SAVED_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L31)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L54) | excludeFromSecondaryId | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L40-L42) | queryId | ID of `ManagedExecution` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L51) | resolvedQuery | `Query` | ␀ |  |  | 
</p></details>



---

## Base FilterValue


Different types of FilterValue can be used by setting `type` to one of the following values:


### BIG_MULTI_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L71)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQBigMultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L45) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L50) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### GROUP<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L148-L152)</sup></sub></sup>
A filter value that consists of multiple inputs that are grouped together into one form. <p> See TestGroupFilter in the tests for an example.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$GroupFilterValue`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L45) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L50) | value | `@NotNull VALUE` | ? |  |  | 
</p></details>

### INTEGER<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L98)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQIntegerFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L45) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L50) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### INTEGER_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L107)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQIntegerRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L45) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L50) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### MONEY_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L116-L119)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQMoneyRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L45) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L50) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### MULTI_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L62)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQMultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L45) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L50) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### REAL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L130)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQRealFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L45) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L50) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### REAL_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L139)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQRealRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L45) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L50) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L80)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L45) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L50) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### STRING<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L89)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQStringFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L45) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L50) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>



---

## Base ResultType


Different types of ResultType can be used by setting `type` to one of the following values:


### BOOLEAN<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/ResultType.java#L78)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.ResultType$BooleanT`

No fields can be set for this type.

</p></details>

### DATE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/ResultType.java#L130)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.ResultType$DateT`

No fields can be set for this type.

</p></details>

### DATE_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/ResultType.java#L152-L155)</sup></sub></sup>
A DateRange is provided by in a query result as two ints in a list, both standing for an epoch day (see {@link LocalDate#toEpochDay()}). The first int describes the included lower bound of the range. The second int descibes the included upper bound.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.ResultType$DateRangeT`

No fields can be set for this type.

</p></details>

### INTEGER<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/ResultType.java#L98)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.ResultType$IntegerT`

No fields can be set for this type.

</p></details>

### LIST<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/ResultType.java#L234)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.ResultType$ListT`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/ResultType.java#L237) | elementType | [@NonNull ResultType](#Base-ResultType) | ? |  |  | 
</p></details>

### MONEY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/ResultType.java#L218)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.ResultType$MoneyT`

No fields can be set for this type.

</p></details>

### NUMERIC<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/ResultType.java#L113)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.ResultType$NumericT`

No fields can be set for this type.

</p></details>

### STRING<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/ResultType.java#L190)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.ResultType$StringT`

No fields can be set for this type.

</p></details>



---

## Base SemanticType


Different types of SemanticType can be used by setting `type` to one of the following values:


### ARX_ATTR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L104-L106)</sup></sub></sup>
Column is annotated with a specific identification type.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.SemanticType$IdentificationT`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L111) | attributeType | `AttributeType` | ? |  |  | 
</p></details>

### CATEGORICAL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L38-L40)</sup></sub></sup>
Column contains a fixed set of String values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.SemanticType$CategoricalT`

No fields can be set for this type.

</p></details>

### CONCEPT_COLUMN<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L90-L94)</sup></sub></sup>
Column contains values used by a Connector of a Concept. Only used for {@link com.bakdata.conquery.apiv1.query.TableExportQuery}.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.SemanticType$ConceptColumnT`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L99) | concept | ID of `@NsIdRef Concept<?>` | ? |  |  | 
</p></details>

### EVENT_DATE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L18-L20)</sup></sub></sup>
Column containing primary Event dates. There should only ever be one EVENT_DATE per Query.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.SemanticType$EventDateT`

No fields can be set for this type.

</p></details>

### ID<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L56-L60)</sup></sub></sup>
Column contains an Entity's Id of a kind. <p> See {@link com.bakdata.conquery.models.config.ColumnConfig} / {@link com.bakdata.conquery.models.config.FrontendConfig.UploadConfig}for the source of this.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.SemanticType$IdT`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L65) | kind | `String` | ? |  |  | 
</p></details>

### RESOLUTION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L47-L49)</sup></sub></sup>
Column contains {@link com.bakdata.conquery.models.forms.util.Resolution} from an {@link com.bakdata.conquery.apiv1.forms.export_form.ExportForm}.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.SemanticType$ResolutionT`

No fields can be set for this type.

</p></details>

### SECONDARY_ID<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L68-L70)</sup></sub></sup>
Column contains values of a {@link SecondaryIdDescription}.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.SemanticType$SecondaryIdT`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L75) | secondaryId | ID of `@NsIdRef SecondaryIdDescription` | ? |  |  | 
</p></details>

### SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L79-L81)</sup></sub></sup>
Column contains the results of a {@link Select}.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.SemanticType$SelectResultT`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L86) | select | ID of `@NsIdRef Select` | ? |  |  | 
</p></details>

### SOURCES<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/types/SemanticType.java#L27-L31)</sup></sub></sup>
Column contains the source of the result line. <p> At the moment, only used in {@link com.bakdata.conquery.apiv1.query.TableExportQuery}.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.types.SemanticType$SourcesT`

No fields can be set for this type.

</p></details>



---

## Other Types

### Type AutocompleteRequest<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L66)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource$AutocompleteRequest`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L71) | page | `@NonNull OptionalInt` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L73) | pageSize | `@NonNull OptionalInt` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L69) | text | `@NonNull Optional<String>` | ? |  |  | 
</p></details>

### Type CQTable<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L25)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.CQTable`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L40) | connector | ID of `@NsIdRef Connector` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L44) | dateColumn | [ValidityDateContainer](#Type-ValidityDateContainer) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L29) | filters | list of [FilterValue<?>](#Base-FilterValue) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L33) | selects | list of ID of `Select` | `[]` |  |  | 
</p></details>

### Type ConceptCodeList<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L67)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource$ConceptCodeList`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L71) | concepts | list of `String` | ? |  |  | 
</p></details>

### Type CurrencyConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L206)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig$CurrencyConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L211) | decimalScale | `int` | `2` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L210) | decimalSeparator | `String` | `","` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L208) | prefix | `String` | `"€"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L209) | thousandSeparator | `String` | `"."` |  |  | 
</p></details>

### Type ExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L19)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.ExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L29) | createdAt | `ZonedDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L43) | finishTime | `LocalDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L37) | id | ID of `ManagedExecution` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L26) | label | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L30) | lastUsed | `ZonedDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L39) | numberOfResults | `long` or `null` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L34) | own | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L31) | owner | ID of `User` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L32) | ownerName | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L45) | queryType | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L40) | requiredTime | `long` or `null` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L49-L51) | resultUrls | list of `URL` | ? |  | The urls under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L46) | secondaryId | ID of `SecondaryIdDescription` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L33) | shared | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L42) | startTime | `LocalDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L38) | status | one of NEW, RUNNING, FAILED, DONE | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L35) | system | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L25) | tags | list of `String` | ? |  |  | 
</p></details>

### Type FERoot<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FERoot.java#L13-L15)</sup></sub></sup>
This class represents the root node of the concepts as it is presented to the front end.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.frontend.FERoot`

No fields can be set for this type.

</p></details>

### Type FEValue<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FEValue.java#L12-L14)</sup></sub></sup>
This class represents a values of a SELECT filter.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.frontend.FEValue`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FEValue.java#L28) | label | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FEValue.java#L31) | optionValue | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FEValue.java#L21-L23) | value | `String` | ? |  | Value is the only relevant data-point for hashing/equality and searching from the service perspective. | 
</p></details>

### Type FilterValues<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L60)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource$FilterValues`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L63) | values | list of `String` | ? |  |  | 
</p></details>

### Type FormConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L50)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L78) | creationTime | `LocalDateTime` | generated default varies |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L59) | dataset | ID of `@NsIdRef Dataset` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L63) | formId | `@NonNull UUID` | generated default varies |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L61) | formType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L65) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L76) | owner | ID of `@MetaIdRef User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L68) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L66) | tags | list of `String` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L70-L73) | values | `@NotNull JsonNode` | `null` |  | This is a blackbox for us at the moment, where the front end saves the state of the formular, when the user saved it. | 
</p></details>

### Type FormConfigFullRepresentation<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L175-L178)</sup></sub></sup>
API representation for a single {@link FormConfig} which includes the form fields an their values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig$FormConfigFullRepresentation`

No fields can be set for this type.

</p></details>

### Type FormConfigOverviewRepresentation<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L151-L154)</sup></sub></sup>
API representation for the overview of all {@link FormConfig}s which does not include the form fields an their values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig$FormConfigOverviewRepresentation`

No fields can be set for this type.

</p></details>

### Type FrontendConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L45)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L55) | currency | [CurrencyConfig](#Type-CurrencyConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L59) | queryUpload | `UploadConfig` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L54) | version | `String` | `"0.0.0-SNAPSHOT"` |  |  | 
</p></details>

### Type FullExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L19-L23)</sup></sub></sup>
This status holds extensive information about the query description and meta data that is computational heavy and can produce a larger payload to requests. It should only be rendered, when a client asks for a specific execution, not if a list of executions is requested.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.FullExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L29) | createdAt | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L43) | finishTime | `LocalDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L37) | id | ID of `ManagedExecution` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L26) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L30) | lastUsed | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L39) | numberOfResults | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L34) | own | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L31) | owner | ID of `User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L32) | ownerName | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L45) | queryType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L40) | requiredTime | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L49-L51) | resultUrls | list of `URL` | `[]` |  | The urls under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L46) | secondaryId | ID of `SecondaryIdDescription` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L33) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L42) | startTime | `LocalDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L38) | status | one of NEW, RUNNING, FAILED, DONE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L35) | system | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L25) | tags | list of `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L63-L65) | availableSecondaryIds | list of ID of `@NsIdRefCollection Set<SecondaryIdDescription>` | `null` |  | Possible {@link SecondaryIdDescription}s available, of {@link com.bakdata.conquery.models.datasets.concepts.Concept}s used in this Query. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L43-L45) | canExpand | `boolean` | `false` |  | Indicates if the concepts that are included in the query description can be accessed by the user. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L38-L40) | columnDescriptions | list of `ColumnDescriptor` | `null` |  | Holds a description for each column, present in the result. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L53-L55) | error | `ConqueryErrorInfo` | `null` |  | Is set when the QueryFailed | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L58-L60) | groups | `Collection<GroupId>` | `null` |  | The groups this execution is shared with. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L30-L34) | progress | `float` or `null` | `null` |  | The estimated progress of an running execution in the enclosing interval [0-1]. This value does not have to be set if the state is RUNNING and it must not be set if the state is not RUNNING. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L48-L50) | query | [QueryDescription](#Base-QueryDescription) | `null` |  | Is set to the query description if the user can expand all included concepts. | 
</p></details>

### Type IdLabel<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/IdLabel.java#L10-L14)</sup></sub></sup>
Container class for the frontend to provide a tuple of id and a corresponding label.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.IdLabel`

No fields can be set for this type.

</p></details>

### Type MetaDataPatch<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L29)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.MetaDataPatch`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L37) | groups | list of ID of `Group` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L36) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L35) | tags | list of `String` | `null` |  |  | 
</p></details>

### Type OverviewExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/OverviewExecutionStatus.java#L5-L7)</sup></sub></sup>
Light weight description of an execution. Rendering the overview should not cause heavy computations.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.OverviewExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L29) | createdAt | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L43) | finishTime | `LocalDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L37) | id | ID of `ManagedExecution` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L26) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L30) | lastUsed | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L39) | numberOfResults | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L34) | own | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L31) | owner | ID of `User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L32) | ownerName | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L45) | queryType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L40) | requiredTime | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L49-L51) | resultUrls | list of `URL` | `[]` |  | The urls under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L46) | secondaryId | ID of `SecondaryIdDescription` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L33) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L42) | startTime | `LocalDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L38) | status | one of NEW, RUNNING, FAILED, DONE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L35) | system | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L25) | tags | list of `String` | `null` |  |  | 
</p></details>

### Type ResolvedConceptsResult<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L356)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptsProcessor$ResolvedConceptsResult`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L361) | resolvedConcepts | `Set<ConceptElementId<?>>` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L362) | resolvedFilter | [ResolvedFilterResult](#Type-ResolvedFilterResult) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L363) | unknownCodes | `Collection<String>` | ? |  |  | 
</p></details>

### Type ResolvedFilterResult<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L346)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptsProcessor$ResolvedFilterResult`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L352) | filterId | ID of `Filter` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L351) | tableId | ID of `Connector` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L353) | value | `Collection<FEValue>` | ? |  |  | 
</p></details>

### Type ValidityDateContainer<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/ValidityDateContainer.java#L9)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.ValidityDateContainer`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/ValidityDateContainer.java#L12) | value | ID of `@NsIdRef ValidityDate` | ? |  |  | 
</p></details>
