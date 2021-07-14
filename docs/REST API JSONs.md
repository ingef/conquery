
# REST API JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.


# REST endpoints

### GET /datasets<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/APIResource.java#L26)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.APIResource`

Method: `getDatasets`

Returns: list of [IdLabel](#Type-IdLabel)

</p></details>

### GET config/frontend<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConfigResource.java#L20)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConfigResource`

Method: `getFrontendConfig`

Returns: [FrontendConfig](#Type-FrontendConfig)

</p></details>

### GET datasets/{dataset}/concepts<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/DatasetResource.java#L25)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.DatasetResource`

Method: `getRoot`

Returns: [FERoot](#Type-FERoot)

</p></details>

### GET datasets/{dataset}/concepts/{concept}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L40)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource`

Method: `getNode`

Returns: `Response`

</p></details>

### POST datasets/{dataset}/concepts/{concept}/resolve<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L53)</sup></sub></sup>


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

Expects: [StringContainer](#Type-StringContainer)
Expects: `OptionalInt`
Expects: `OptionalInt`
Returns: list of [FEValue](#Type-FEValue)

</p></details>

### POST datasets/{dataset}/concepts/{concept}/tables/{table}/filters/{filter}/resolve<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L38)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource`

Method: `resolveFilterValues`

Expects: [FilterValues](#Type-FilterValues)
Returns: [ResolvedConceptsResult](#Type-ResolvedConceptsResult)

</p></details>

### GET datasets/{dataset}/queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L46)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `getAllQueries`

Expects: `Optional<Boolean>`
Returns: list of [ExecutionStatus](#Type-ExecutionStatus)

</p></details>

### POST datasets/{dataset}/queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L55)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `postQuery`

Expects: `Optional<Boolean>`
Expects: [@NotNull(message="{javax.validation.constraints.NotNull.message}", groups={}, payload={}) @Valid QueryDescription](#Base-QueryDescription)
Returns: `Response`

</p></details>

### GET datasets/{dataset}/queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L67)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `getStatus`

Expects: `Optional<Boolean>`
Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### PATCH datasets/{dataset}/queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L80)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `patchQuery`

Expects: `Optional<Boolean>`
Expects: [MetaDataPatch](#Type-MetaDataPatch)
Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### DELETE datasets/{dataset}/queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L91)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `deleteQuery`

Returns: `void`

</p></details>

### POST datasets/{dataset}/queries/{query}/cancel<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L110)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `cancel`

Returns: `void`

</p></details>

### POST datasets/{dataset}/queries/{query}/reexecute<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L100)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `reexecute`

Expects: `Optional<Boolean>`
Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### GET datasets/{dataset}/result/{query}.csv<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ResultCsvResource.java#L52)</sup></sub></sup>


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


### ABSOLUTE_FORM_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L30)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L37) | dateRange | `@NotNull @Valid Range<LocalDate>` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L39) | features | [ARRAY_CONCEPT_QUERY](#ARRAY_CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L35) | query | `@NotNull @Valid Query` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L41) | resolutionsAndAlignmentMap | list of `ResolutionAndAlignment` | ? |  |  | 
</p></details>

### ARRAY_CONCEPT_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L32-L36)</sup></sub></sup>
Query type that combines a set of {@link ConceptQuery}s which are separately evaluated and whose results are merged. If a SpecialDateUnion is required, the result will hold the union of all dates from the separate queries.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.ArrayConceptQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L44) | childQueries | list of [CONCEPT_QUERY](#CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L47) | dateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L51) | resolvedDateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
</p></details>

### CONCEPT_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L27)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.ConceptQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L38) | dateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L42) | resolvedDateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L34) | root | [@Valid @NotNull CQElement](#Base-CQElement) | ? |  |  | 
</p></details>

### ENTITY_DATE_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/EntityDateQuery.java#L29-L33)</sup></sub></sup>
This query uses the date range defined by {@link EntityDateQuery#query} for each entity and applies it as a date restriction for the following query defined by {@link EntityDateQuery#features}. The additional {@link EntityDateQuery#dateRange} is applied globally on all entities.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.EntityDateQuery`

No fields can be set for this type.

</p></details>

### EXPORT_FORM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L44)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.forms.export_form.ExportForm`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L61) | alsoCreateCoarserSubdivisions | `boolean` | `true` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L48) | queryGroupId | ID of `ManagedExecution` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L58) | resolution | list of one of COMPLETE, YEARS, QUARTERS, DAYS | `["COMPLETE"]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L55) | timeMode | `@NotNull @Valid Mode` | `null` |  |  | 
</p></details>

### FULL_EXPORT_FORM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L44)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.forms.export_form.FullExportForm`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L56) | dateRange | `@Valid Range<LocalDate>` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L49) | queryGroupId | ID of `ManagedExecution` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L60) | tables | list of [CQElement](#Base-CQElement) | `[]` |  |  | 
</p></details>

### RELATIVE_FORM_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L31)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.RelativeFormQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L37) | features | [ARRAY_CONCEPT_QUERY](#ARRAY_CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L43) | indexPlacement | one of BEFORE, NEUTRAL, AFTER | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L41) | indexSelector | one of EARLIEST, LATEST, RANDOM | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L39) | outcomes | [ARRAY_CONCEPT_QUERY](#ARRAY_CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L35) | query | `@NotNull @Valid Query` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L51) | resolutionsAndAlignmentMap | list of `ResolutionAndAlignment` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L47) | timeCountAfter | `@javax.validation.constraints.Min(0) int` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L45) | timeCountBefore | `@javax.validation.constraints.Min(0) int` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L49) | timeUnit | one of DAYS, QUARTERS, YEARS | ? |  |  | 
</p></details>

### SECONDARY_ID_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L37)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.SecondaryIdQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L50) | dateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | `"MERGE"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L54-L56) | query | [CONCEPT_QUERY](#CONCEPT_QUERY) | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L43) | root | [@NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L46) | secondaryId | ID of `@NsIdRef @NotNull SecondaryIdDescription` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L60) | withSecondaryId | list of ID of `@NsIdRefCollection Set<Column>` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L64) | withoutSecondaryId | list of ID of `@NsIdRefCollection Set<Table>` | ␀ |  |  | 
</p></details>

### TABLE_EXPORT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L49-L51)</sup></sub></sup>
A TABLE_EXPORT creates a full export of the given tables. It ignores selects completely.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.TableExportQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L63) | dateRange | `@NotNull Range<LocalDate>` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L69) | positions | map from `Column` to `int` or `null` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L59) | query | `@NonNull Query` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L65) | tables | list of `CQUnfilteredTable` | ? |  |  | 
</p></details>



---

## Base CQElement


Different types of CQElement can be used by setting `type` to one of the following values:


### AND<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L36)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQAnd`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L39) | children | list of [CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L45) | createExists | `Optional<Boolean>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L48) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQBeforeTemporalQuery.java#L12-L14)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQBeforeTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### BEFORE_OR_SAME<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQBeforeOrSameTemporalQuery.java#L12-L14)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened on the same day or before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQBeforeOrSameTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### CONCEPT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L48)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQConcept`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L76) | aggregateEventDates | `@javax.validation.constraints.NotNull boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L55-L57) | elements | list of ID of `ConceptElement<?>` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L74) | excludeFromSecondaryIdQuery | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L73) | excludeFromTimeAggregation | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L69) | selects | list of ID of `Select` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L63) | tables | list of [CQTable](#Type-CQTable) | `[]` |  |  | 
</p></details>

### DATE_RESTRICTION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQDateRestriction.java#L30)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQDateRestriction.java#L36) | child | [@Valid @NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQDateRestriction.java#L34) | dateRange | `@NotNull Range<LocalDate>` | `null` |  |  | 
</p></details>

### DAYS_BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQDaysBeforeTemporalQuery.java#L14-L16)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQDaysBeforeTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### DAYS_OR_NO_EVENT_BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQDaysBeforeOrNeverTemporalQuery.java#L13-L15)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}, or no events. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQDaysBeforeOrNeverTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### EXTERNAL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQExternal.java#L42)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQExternal`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | ? |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQExternal.java#L47) | format | list of one of ID, EVENT_DATE, START_DATE, END_DATE, DATE_RANGE, DATE_SET, IGNORE | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQExternal.java#L51) | values | list of `String` | ? |  |  | 
</p></details>

### NEGATION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQNegation.java#L23)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQNegation`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQNegation.java#L28) | child | [@Valid @NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQNegation.java#L31) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### OR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L38)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQOr`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L42) | children | list of [CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L48) | createExists | `Optional<Boolean>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L52) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### RESULT_INFO_DECORATOR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/ResultInfoDecorator.java#L25-L27)</sup></sub></sup>
A wrapper for {@link CQElement}s to provide additional infos to parts of a query.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.ResultInfoDecorator`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/ResultInfoDecorator.java#L38) | child | [@NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/ResultInfoDecorator.java#L36) | values | ClassToInstanceMap maps from base class `Object` to instances of subtypes |  |  |  | 
</p></details>

### SAME<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQSameTemporalQuery.java#L12-L14)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} at the same time as the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQSameTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### SAVED_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L29)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L31-L33) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L52) | excludeFromSecondaryId | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L38-L40) | queryId | ID of `ManagedExecution` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L49) | resolvedQuery | `Query` | ␀ |  |  | 
</p></details>



---

## Base FilterValue


Different types of FilterValue can be used by setting `type` to one of the following values:


### BIG_MULTI_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L52)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQBigMultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### INTEGER_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L76)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQIntegerRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### MONEY_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L84-L87)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQMoneyRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### MULTI_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L44)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQMultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### REAL_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L95)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQRealRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L60)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### STRING<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L68)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQStringFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>



---

## Other Types

### Type CQTable<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L21)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.CQTable`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L35) | connector | ID of `@NsIdRef Connector` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L39) | dateColumn | [ValidityDateContainer](#Type-ValidityDateContainer) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L24) | filters | list of [FilterValue<?>](#Base-FilterValue) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L28) | selects | list of ID of `Select` | `[]` |  |  | 
</p></details>

### Type ConceptCodeList<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L64)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource$ConceptCodeList`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L68) | concepts | list of `String` | `null` |  |  | 
</p></details>

### Type CurrencyConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L20)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig$CurrencyConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L26) | decimalScale | `int` | `2` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L25) | decimalSeparator | `String` | `","` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L23) | prefix | `String` | `"€"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L24) | thousandSeparator | `String` | `"."` |  |  | 
</p></details>

### Type ExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L18)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.ExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L28) | createdAt | `ZonedDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L36) | id | ID of `ManagedExecution` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L25) | label | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L29) | lastUsed | `ZonedDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L38) | numberOfResults | `long` or `null` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L33) | own | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L30) | owner | ID of `User` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L31) | ownerName | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L41) | queryType | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L39) | requiredTime | `long` or `null` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L45-L47) | resultUrls | list of `URL` | ? |  | The urls under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L42) | secondaryId | ID of `SecondaryIdDescription` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L32) | shared | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L37) | status | one of NEW, RUNNING, FAILED, DONE | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L34) | system | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L24) | tags | list of `String` | ? |  |  | 
</p></details>

### Type FERoot<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FERoot.java#L11-L13)</sup></sub></sup>
This class represents the root node of the concepts as it is presented to the front end.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.frontend.FERoot`

No fields can be set for this type.

</p></details>

### Type FEValue<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FEValue.java#L8-L10)</sup></sub></sup>
This class represents a values of a SELECT filter.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.frontend.FEValue`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FEValue.java#L17) | optionValue | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FEValue.java#L16) | templateValues | map from `String` to `String` | ? |  |  | 
</p></details>

### Type FilterValues<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L58)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource$FilterValues`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L60) | values | list of `String` | `null` |  |  | 
</p></details>

### Type FormConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L49)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L77) | creationTime | `LocalDateTime` | generated default varies |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L58) | dataset | ID of `@NsIdRef Dataset` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L62) | formId | `@NonNull UUID` | generated default varies |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L60) | formType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L64) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L75) | owner | ID of `@MetaIdRef User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L67) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L65) | tags | list of `String` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L69-L72) | values | `@NotNull JsonNode` | `null` |  | This is a blackbox for us at the moment, where the front end saves the state of the formular, when the user saved it. | 
</p></details>

### Type FormConfigFullRepresentation<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L174-L177)</sup></sub></sup>
API representation for a single {@link FormConfig} which includes the form fields an their values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig$FormConfigFullRepresentation`

No fields can be set for this type.

</p></details>

### Type FormConfigOverviewRepresentation<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L150-L153)</sup></sub></sup>
API representation for the overview of all {@link FormConfig}s which does not include the form fields an their values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig$FormConfigOverviewRepresentation`

No fields can be set for this type.

</p></details>

### Type FrontendConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L13)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L17) | currency | [CurrencyConfig](#Type-CurrencyConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L16) | version | `String` | `"0.0.0-SNAPSHOT"` |  |  | 
</p></details>

### Type FullExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L20-L24)</sup></sub></sup>
This status holds extensive information about the query description and meta data that is computational heavy and can produce a larger payload to requests. It should only be rendered, when a client asks for a specific execution, not if a list of executions is requested.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.FullExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L28) | createdAt | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L36) | id | ID of `ManagedExecution` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L25) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L29) | lastUsed | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L38) | numberOfResults | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L33) | own | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L30) | owner | ID of `User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L31) | ownerName | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L41) | queryType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L39) | requiredTime | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L45-L47) | resultUrls | list of `URL` | `[]` |  | The urls under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L42) | secondaryId | ID of `SecondaryIdDescription` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L32) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L37) | status | one of NEW, RUNNING, FAILED, DONE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L34) | system | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L24) | tags | list of `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L64-L66) | availableSecondaryIds | list of ID of `@NsIdRefCollection Set<SecondaryIdDescription>` | `null` |  | Possible {@link SecondaryIdDescription}s available, of {@link com.bakdata.conquery.models.datasets.concepts.Concept}s used in this Query. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L44-L46) | canExpand | `boolean` | `false` |  | Indicates if the concepts that are included in the query description can be accessed by the user. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L39-L41) | columnDescriptions | list of `ColumnDescriptor` | `null` |  | Holds a description for each column, present in the result. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L54-L56) | error | `ConqueryErrorInfo` | `null` |  | Is set when the QueryFailed | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L59-L61) | groups | `Collection<GroupId>` | `null` |  | The groups this execution is shared with. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L31-L35) | progress | `float` or `null` | `null` |  | The estimated progress of an running execution in the enclosing interval [0-1]. This value does not have to be set if the state is RUNNING and it must not be set if the state is not RUNNING. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L49-L51) | query | [QueryDescription](#Base-QueryDescription) | `null` |  | Is set to the query description if the user can expand all included concepts. | 
</p></details>

### Type IdLabel<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/IdLabel.java#L13-L16)</sup></sub></sup>
Container class for the frontend to provide a tuple of id and a corresponding label.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.IdLabel`

No fields can be set for this type.

</p></details>

### Type MetaDataPatch<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L30)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.MetaDataPatch`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L38) | groups | list of ID of `Group` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L37) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L36) | tags | list of `String` | `null` |  |  | 
</p></details>

### Type OverviewExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/OverviewExecutionStatus.java#L5-L7)</sup></sub></sup>
Light weight description of an execution. Rendering the overview should not cause heavy computations.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.OverviewExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L28) | createdAt | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L36) | id | ID of `ManagedExecution` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L25) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L29) | lastUsed | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L38) | numberOfResults | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L33) | own | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L30) | owner | ID of `User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L31) | ownerName | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L41) | queryType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L39) | requiredTime | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L45-L47) | resultUrls | list of `URL` | `[]` |  | The urls under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L42) | secondaryId | ID of `SecondaryIdDescription` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L32) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L37) | status | one of NEW, RUNNING, FAILED, DONE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L34) | system | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L24) | tags | list of `String` | `null` |  |  | 
</p></details>

### Type ResolvedConceptsResult<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L260)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptsProcessor$ResolvedConceptsResult`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L265) | resolvedConcepts | list of ID of `ConceptElement` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L266) | resolvedFilter | [ResolvedFilterResult](#Type-ResolvedFilterResult) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L267) | unknownCodes | list of `String` | ? |  |  | 
</p></details>

### Type ResolvedFilterResult<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L250)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptsProcessor$ResolvedFilterResult`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L256) | filterId | ID of `Filter` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L255) | tableId | ID of `Connector` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L257) | value | list of [FEValue](#Type-FEValue) | ? |  |  | 
</p></details>

### Type StringContainer<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L63)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource$StringContainer`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L65) | text | `String` | `null` |  |  | 
</p></details>

### Type ValidityDateContainer<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/ValidityDateContainer.java#L9)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.ValidityDateContainer`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/ValidityDateContainer.java#L12) | value | ID of `@NsIdRef ValidityDate` | ? |  |  | 
</p></details>
