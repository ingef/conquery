
# Concept JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.

Each `*.concept.json` has to contain exactly one [Concept](#Base-Concept).


---

## Base Concept
A concept is a collection of filters and selects and their connection to tables.

Different types of Concept can be used by setting `type` to one of the following values:


### TREE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/TreeConcept.java#L36-L38)</sup></sub></sup>
This is a single node or concept in a concept tree.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.tree.TreeConcept`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java#L35) | connectors | list of [Connector](#Type-Connector) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java#L37) | dataset | ID of `Dataset` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java#L33) | hidden | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java#L21) | additionalInfos | list of [KeyValue](#Type-KeyValue) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java#L19) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/TreeConcept.java#L55) | children | list of [ConceptTreeChild](#Type-ConceptTreeChild) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/TreeConcept.java#L62) | selects | list of [UniversalSelect](#Marker-UniversalSelect) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### VIRTUAL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/virtual/VirtualConcept.java#L16-L18)</sup></sub></sup>
This is a single node or concept in a concept tree.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.virtual.VirtualConcept`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java#L35) | connectors | list of [Connector](#Type-Connector) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java#L37) | dataset | ID of `Dataset` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java#L33) | hidden | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java#L21) | additionalInfos | list of [KeyValue](#Type-KeyValue) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java#L19) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/virtual/VirtualConcept.java#L22) | selects | list of [Select](#Base-Select) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>



---

## Base CTCondition
These represent guard conditions. A value matches a [ConceptElement](#ConceptElement) if it matches its condition and its parent

Different types of CTCondition can be used by setting `type` to one of the following values:


### AND<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/AndCondition.java#L16-L18)</sup></sub></sup>
This condition connects multiple conditions with an and.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.conditions.AndCondition`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/AndCondition.java#L22) | conditions | list of [CTCondition](#Base-CTCondition) | `null` |  |  | 
</p></details>

### COLUMN_EQUAL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/ColumnEqualCondition.java#L17-L19)</sup></sub></sup>
This condition requires the value of another column to be equal to a given value.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.conditions.ColumnEqualCondition`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/ColumnEqualCondition.java#L26) | column | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/ColumnEqualCondition.java#L24) | values | `@NotEmpty Set<String>` | ? |  |  | 
</p></details>

### EQUAL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/EqualCondition.java#L16-L18)</sup></sub></sup>
This condition requires each value to be exactly as given in the list.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.conditions.EqualCondition`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/EqualCondition.java#L23) | values | `@NotEmpty Set<String>` | ? |  |  | 
</p></details>

### GROOVY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/GroovyCondition.java#L25-L27)</sup></sub></sup>
A condition that is a groovy script and thus able to represent everything.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.conditions.GroovyCondition`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/GroovyCondition.java#L36) | script | `String` | `null` |  |  | 
</p></details>

### NOT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/NotCondition.java#L15-L17)</sup></sub></sup>
This condition matches if its child does not.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.conditions.NotCondition`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/NotCondition.java#L21) | condition | [@Valid @NotEmpty CTCondition](#Base-CTCondition) | `null` |  |  | 
</p></details>

### OR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/OrCondition.java#L16-L18)</sup></sub></sup>
This condition connects multiple conditions with an or.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.conditions.OrCondition`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/OrCondition.java#L22) | conditions | list of [CTCondition](#Base-CTCondition) | `null` |  |  | 
</p></details>

### PREFIX_LIST<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixCondition.java#L13-L15)</sup></sub></sup>
This condition requires each value to start with one of the given values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.conditions.PrefixCondition`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixCondition.java#L20) | prefixes | list of `String` | `null` |  |  | 
</p></details>

### PREFIX_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixRangeCondition.java#L14-L16)</sup></sub></sup>
This condition requires each value to start with a prefix between the two given values

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.conditions.PrefixRangeCondition`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixRangeCondition.java#L22) | max | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixRangeCondition.java#L20) | min | `String` | `null` |  |  | 
</p></details>

### PRESENT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/IsPresentCondition.java#L11-L13)</sup></sub></sup>
This condition requires that the selected Column has a value.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.conditions.IsPresentCondition`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/IsPresentCondition.java#L17) | column | `String` | `null` |  |  | 
</p></details>



---

## Base Filter
These are used to define filters, than can be used to reduce the result set.

Different types of Filter can be used by setting `type` to one of the following values:


### BIG_MULTI_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/BigMultiSelectFilter.java#L12-L14)</sup></sub></sup>
This filter represents a select in the front end. This means that the user can select one or more values from a list of values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.filters.specific.BigMultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L35) | allowDropFile | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L31) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L34) | pattern | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L30) | unit | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java#L14) | column | ID of `@Valid @NotNull @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L35-L37) | labels | bijective map from `String` to `String` |  |  | user given mapping from the values in the CSVs to shown labels | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L57) | searchType | one of PREFIX, CONTAINS, EXACT | `"EXACT"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L49) | template | [FilterTemplate](#Type-FilterTemplate) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L40) | values | `Set<String>` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### COUNT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountFilter.java#L23-L25)</sup></sub></sup>
This filter represents a select in the front end. This means that the user can select one or more values from a list of values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.filters.specific.CountFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L35) | allowDropFile | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L31) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L34) | pattern | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L30) | unit | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountFilter.java#L31) | column | ID of `@Valid @NotNull @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountFilter.java#L36) | distinct | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountFilter.java#L39) | distinctByColumn | list of ID of `@Valid @NsIdRefCollection Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### COUNT_QUARTERS<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountQuartersFilter.java#L19)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.filters.specific.CountQuartersFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L35) | allowDropFile | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L31) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L34) | pattern | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L30) | unit | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java#L14) | column | ID of `@Valid @NotNull @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### DATE_DISTANCE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/DateDistanceFilter.java#L22-L24)</sup></sub></sup>
This filter represents a select in the front end. This means that the user can select one or more values from a list of values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.filters.specific.DateDistanceFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L35) | allowDropFile | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L31) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L34) | pattern | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L30) | unit | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java#L14) | column | ID of `@Valid @NotNull @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/DateDistanceFilter.java#L29) | timeUnit | one of NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, MONTHS, YEARS, DECADES, CENTURIES, MILLENNIA, ERAS, FOREVER | `"YEARS"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### DURATION_SUM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/DurationSumFilter.java#L20)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.filters.specific.DurationSumFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L35) | allowDropFile | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L31) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L34) | pattern | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L30) | unit | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java#L14) | column | ID of `@Valid @NotNull @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### NUMBER<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/NumberFilter.java#L24-L26)</sup></sub></sup>
This filter represents a filter on an integer columnof each event.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.filters.specific.NumberFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L35) | allowDropFile | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L31) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L34) | pattern | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L30) | unit | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java#L14) | column | ID of `@Valid @NotNull @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### PREFIX_TEXT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/PrefixTextFilter.java#L16)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.filters.specific.PrefixTextFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L35) | allowDropFile | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L31) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L34) | pattern | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L30) | unit | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java#L14) | column | ID of `@Valid @NotNull @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### QUARTERS_IN_YEAR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/QuartersInYearFilter.java#L18)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.filters.specific.QuartersInYearFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L35) | allowDropFile | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L31) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L34) | pattern | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L30) | unit | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java#L14) | column | ID of `@Valid @NotNull @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/MultiSelectFilter.java#L12-L14)</sup></sub></sup>
This filter represents a select in the front end. This means that the user can select one or more values from a list of values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.filters.specific.MultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L35) | allowDropFile | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L31) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L34) | pattern | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L30) | unit | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java#L14) | column | ID of `@Valid @NotNull @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L35-L37) | labels | bijective map from `String` to `String` |  |  | user given mapping from the values in the CSVs to shown labels | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L57) | searchType | one of PREFIX, CONTAINS, EXACT | `"EXACT"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L49) | template | [FilterTemplate](#Type-FilterTemplate) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L40) | values | `Set<String>` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### SINGLE_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SelectFilter.java#L11-L15)</sup></sub></sup>
This filter represents a select in the front end. This means that the user can select one or more values from a list of values.",

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.filters.specific.SelectFilter`

Example:

```jsonc
{
	"label": "gender",
	"column": "reference_data.gender",
	"type": "SELECT"
}
```

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L35) | allowDropFile | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L31) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L34) | pattern | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L30) | unit | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java#L14) | column | ID of `@Valid @NotNull @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L35-L37) | labels | bijective map from `String` to `String` |  |  | user given mapping from the values in the CSVs to shown labels | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L57) | searchType | one of PREFIX, CONTAINS, EXACT | `"EXACT"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L49) | template | [FilterTemplate](#Type-FilterTemplate) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java#L40) | values | `Set<String>` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### SUM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter.java#L32-L34)</sup></sub></sup>
This filter represents a filter on the sum of one integer column.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.filters.specific.SumFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L35) | allowDropFile | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L31) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L34) | pattern | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java#L30) | unit | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter.java#L42) | column | ID of `@Valid @NotNull @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter.java#L55) | distinctByColumn | ID of `@Valid @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter.java#L49) | subtractColumn | ID of `@Valid @NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>



---

## Base Select
These are used to define selects, that can be used to create additional CSV columns.

Different types of Select can be used by setting `type` to one of the following values:


### COUNT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountSelect.java#L16)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.CountSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountSelect.java#L29) | column | ID of `@NsIdRef @NotNull Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountSelect.java#L20) | distinct | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountSelect.java#L24) | distinctByColumn | ID of `@NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### COUNT_OCCURENCES<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountOccurencesSelect.java#L18)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.CountOccurencesSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L25-L28) | categorical | `boolean` | ? |  | Indicates if the values in the specified column belong to a categorical set (bounded number of values). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L18) | column | ID of `@NonNull Column` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountOccurencesSelect.java#L21) | selection | list of `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### COUNT_QUARTERS<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountQuartersSelect.java#L13-L16)</sup></sub></sup>
Entity is included when the number of distinct quarters for all events is within a given range. Implementation is specific for DateRanges

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.CountQuartersSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L25-L28) | categorical | `boolean` | ? |  | Indicates if the values in the specified column belong to a categorical set (bounded number of values). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L18) | column | ID of `@NonNull Column` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### DATE_DISTANCE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/DateDistanceSelect.java#L17)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.DateDistanceSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L25-L28) | categorical | `boolean` | ? |  | Indicates if the values in the specified column belong to a categorical set (bounded number of values). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L18) | column | ID of `@NonNull Column` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/DateDistanceSelect.java#L26) | timeUnit | one of NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, MONTHS, YEARS, DECADES, CENTURIES, MILLENNIA, ERAS, FOREVER | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### DATE_UNION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/DateUnionSelect.java#L12)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.DateUnionSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L25-L28) | categorical | `boolean` | ? |  | Indicates if the values in the specified column belong to a categorical set (bounded number of values). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L18) | column | ID of `@NonNull Column` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### DISTINCT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/DistinctSelect.java#L11)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.DistinctSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L25-L28) | categorical | `boolean` | ? |  | Indicates if the values in the specified column belong to a categorical set (bounded number of values). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L18) | column | ID of `@NonNull Column` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### DURATION_SUM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/DurationSumSelect.java#L12)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.DurationSumSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L25-L28) | categorical | `boolean` | ? |  | Indicates if the values in the specified column belong to a categorical set (bounded number of values). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L18) | column | ID of `@NonNull Column` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### EVENT_DATE_UNION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/concept/specific/EventDateUnionSelect.java#L17-L20)</sup></sub></sup>
Collects the event dates that are corresponding to an enclosing {@link Connector} or {@link Concept} provided in a query. The resulting date set is in bounds of a provided date restriction.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.concept.specific.EventDateUnionSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `"ResultHeadersC10n.dates"` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `"resultheadersc10n_dates"` |  |  | 
</p></details>

### EXISTS<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/concept/specific/ExistsSelect.java#L17)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.concept.specific.ExistsSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### FIRST<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/FirstValueSelect.java#L11)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.FirstValueSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L25-L28) | categorical | `boolean` | ? |  | Indicates if the values in the specified column belong to a categorical set (bounded number of values). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L18) | column | ID of `@NonNull Column` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### LAST<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/LastValueSelect.java#L11)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.LastValueSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L25-L28) | categorical | `boolean` | ? |  | Indicates if the values in the specified column belong to a categorical set (bounded number of values). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L18) | column | ID of `@NonNull Column` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### PREFIX<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/PrefixSelect.java#L15)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.PrefixSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L25-L28) | categorical | `boolean` | ? |  | Indicates if the values in the specified column belong to a categorical set (bounded number of values). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L18) | column | ID of `@NonNull Column` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/PrefixSelect.java#L18) | prefix | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### QUARTERS_IN_YEAR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/QuartersInYearSelect.java#L12-L14)</sup></sub></sup>
Entity is included when the the number of quarters with events is within a specified range.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.QuartersInYearSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L25-L28) | categorical | `boolean` | ? |  | Indicates if the values in the specified column belong to a categorical set (bounded number of values). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L18) | column | ID of `@NonNull Column` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### RANDOM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/RandomValueSelect.java#L11)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.RandomValueSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L25-L28) | categorical | `boolean` | ? |  | Indicates if the values in the specified column belong to a categorical set (bounded number of values). | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java#L18) | column | ID of `@NonNull Column` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### SUM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect.java#L25)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.SumSelect`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java#L26) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect.java#L36) | column | ID of `@NsIdRef @NotNull Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect.java#L31) | distinctByColumn | ID of `@NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect.java#L40) | subtractColumn | ID of `@NsIdRef Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>



---

## Other Types

### Type ConceptTreeChild<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/ConceptTreeChild.java#L19)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.tree.ConceptTreeChild`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java#L21) | additionalInfos | list of [KeyValue](#Type-KeyValue) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java#L19) | description | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/ConceptTreeChild.java#L23) | children | list of [ConceptTreeChild](#Type-ConceptTreeChild) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/ConceptTreeChild.java#L39) | condition | [@NotNull CTCondition](#Base-CTCondition) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

### Type Connector<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Connector.java#L39-L41)</sup></sub></sup>
A connector represents the connection between a column and a concept.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.Connector`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Connector.java#L55) | selects | list of [Select](#Base-Select) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Connector.java#L48) | validityDates | list of [ValidityDate](#Type-ValidityDate) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | ? | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | ? |  |  | 
</p></details>

### Type FilterTemplate<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java#L9)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.FilterTemplate`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java#L23-L25) | columnValue | `String` | ? |  | Value to Filter. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java#L19-L21) | columns | list of `String` | ? |  | Columns to search see {@link FilterSearch}. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java#L15-L17) | filePath | `String` | ? |  | Path to CSV File. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java#L31-L33) | optionValue | `String` | ? |  | Option value. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java#L27-L29) | value | `String` | ? |  | Selected value. | 
</p></details>

### Type KeyValue<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/common/KeyValue.java#L7-L9)</sup></sub></sup>
This class represents a simple Key-Value pair as it is used in the additionInfos field.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.common.KeyValue`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/common/KeyValue.java#L15) | key | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/common/KeyValue.java#L16) | value | `String` | `null` |  |  | 
</p></details>

### Type ValidityDate<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ValidityDate.java#L15)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.ValidityDate`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ValidityDate.java#L18) | column | ID of `@NsIdRef @NotNull Column` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L20-L23) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
</p></details>

---

## Marker Interfaces

### Marker UniversalSelect<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/concept/UniversalSelect.java#L5)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.concepts.select.concept.UniversalSelect`

A Marker UniversalSelect is any of:
* [EVENT_DATE_UNION](#EVENT_DATE_UNION)
* [EXISTS](#EXISTS)

</p></details>
