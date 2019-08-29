
# Concept JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.

Each `*.concept.json` has to contain exactly one [Concept](#Concept).


---

## Base Concept
A concept is collections of filters and selects and their connection to tables.

Different types of Concept can be used by setting `type` to one of the following values:


### TREE
Java Type: `com.bakdata.conquery.models.concepts.tree.TreeConcept`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/TreeConcept.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java) | connectors | list of [Connector](#Type-Connector) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java) | dataset | ID of `Dataset` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java) | hidden | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java) | additionalInfos | list of [KeyValue](#Type-KeyValue) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/TreeConcept.java) | children | list of [ConceptTreeChild](#Type-ConceptTreeChild) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/TreeConcept.java) | globalToLocalOffset | `int` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/TreeConcept.java) | selects | list of `UniversalSelect` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### VIRTUAL
Java Type: `com.bakdata.conquery.models.concepts.virtual.VirtualConcept`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/virtual/VirtualConcept.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java) | connectors | list of [Connector](#Type-Connector) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java) | dataset | ID of `Dataset` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Concept.java) | hidden | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java) | additionalInfos | list of [KeyValue](#Type-KeyValue) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/virtual/VirtualConcept.java) | selects | list of [Select](#Base-Select) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 



---

## Base CTCondition
These represent guard conditions. A value matches a [ConceptElement](#ConceptElement) if it matches its condition and its parent

Different types of CTCondition can be used by setting `type` to one of the following values:


### AND
Java Type: `com.bakdata.conquery.models.concepts.conditions.AndCondition`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/AndCondition.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/AndCondition.java) | conditions | list of [CTCondition](#Base-CTCondition) |  |  | 

### COLUMN_EQUAL
Java Type: `com.bakdata.conquery.models.concepts.conditions.ColumnEqualCondition`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/ColumnEqualCondition.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/ColumnEqualCondition.java) | column | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/ColumnEqualCondition.java) | values | list of `String` |  |  | 

### EQUAL
Java Type: `com.bakdata.conquery.models.concepts.conditions.EqualCondition`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/EqualCondition.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/EqualCondition.java) | values | list of `String` |  |  | 

### GROOVY
Java Type: `com.bakdata.conquery.models.concepts.conditions.GroovyCondition`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/GroovyCondition.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/GroovyCondition.java) | script | `String` |  |  | 

### NOT
Java Type: `com.bakdata.conquery.models.concepts.conditions.NotCondition`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/NotCondition.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/NotCondition.java) | condition | [CTCondition](#Base-CTCondition) |  |  | 

### OR
Java Type: `com.bakdata.conquery.models.concepts.conditions.OrCondition`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/OrCondition.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/OrCondition.java) | conditions | list of [CTCondition](#Base-CTCondition) |  |  | 

### PREFIX_LIST
Java Type: `com.bakdata.conquery.models.concepts.conditions.PrefixCondition`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixCondition.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixCondition.java) | prefixes | list of `String` |  |  | 

### PREFIX_RANGE
Java Type: `com.bakdata.conquery.models.concepts.conditions.PrefixRangeCondition`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixRangeCondition.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixRangeCondition.java) | max | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixRangeCondition.java) | min | `String` |  |  | 



---

## Base Filter
These are used to define filters, than can be used to reduce the result set.

Different types of Filter can be used by setting `type` to one of the following values:


### BIG_MULTI_SELECT
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.BigMultiSelectFilter`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/BigMultiSelectFilter.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | allowDropFile | boolean or null |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | pattern | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | template | [FilterTemplate](#Type-FilterTemplate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | unit | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java) | labels | bijective map from `String` to `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java) | values | list of `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### COUNT
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.CountFilter`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountFilter.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | allowDropFile | boolean or null |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | pattern | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | template | [FilterTemplate](#Type-FilterTemplate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | unit | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountFilter.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountFilter.java) | distinct | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountFilter.java) | distinctByColumn | list of ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### COUNT_QUARTERS
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.CountQuartersFilter`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountQuartersFilter.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | allowDropFile | boolean or null |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | pattern | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | template | [FilterTemplate](#Type-FilterTemplate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | unit | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### DATE_DISTANCE
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.DateDistanceFilter`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/DateDistanceFilter.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | allowDropFile | boolean or null |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | pattern | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | template | [FilterTemplate](#Type-FilterTemplate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | unit | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/DateDistanceFilter.java) | timeUnit | one of NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, MONTHS, YEARS, DECADES, CENTURIES, MILLENNIA, ERAS, FOREVER |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### DURATION_SUM
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.DurationSumFilter`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/DurationSumFilter.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | allowDropFile | boolean or null |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | pattern | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | template | [FilterTemplate](#Type-FilterTemplate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | unit | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### NUMBER
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.NumberFilter`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/NumberFilter.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | allowDropFile | boolean or null |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | pattern | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | template | [FilterTemplate](#Type-FilterTemplate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | unit | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### PREFIX_TEXT
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.PrefixTextFilter`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/PrefixTextFilter.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | allowDropFile | boolean or null |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | pattern | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | template | [FilterTemplate](#Type-FilterTemplate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | unit | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### QUARTERS_IN_YEAR
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.QuartersInYearFilter`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/QuartersInYearFilter.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | allowDropFile | boolean or null |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | pattern | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | template | [FilterTemplate](#Type-FilterTemplate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | unit | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### SELECT
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.MultiSelectFilter`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/MultiSelectFilter.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | allowDropFile | boolean or null |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | pattern | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | template | [FilterTemplate](#Type-FilterTemplate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | unit | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java) | labels | bijective map from `String` to `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java) | values | list of `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### SINGLE_SELECT
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.SelectFilter`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SelectFilter.java) This filter represents a select in the front end. This means that the user can select one or more values from a list of values.

Example:

```jsonc
{
	"label": "gender",
	"column": "reference_data.gender",
	"type": "SELECT"
}
```

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | allowDropFile | boolean or null |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | pattern | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | template | [FilterTemplate](#Type-FilterTemplate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | unit | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java) | labels | bijective map from `String` to `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter.java) | values | list of `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### SUM
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.SumFilter`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | allowDropFile | boolean or null |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | pattern | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | template | [FilterTemplate](#Type-FilterTemplate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter.java) | unit | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter.java) | distinct | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter.java) | distinctByColumn | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter.java) | subtractColumn | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 



---

## Base Select
These are used to define selects, that can be used to create additional CSV columns.

Different types of Select can be used by setting `type` to one of the following values:


### COUNT
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.CountSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountSelect.java) | distinct | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountSelect.java) | distinctByColumn | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### COUNT_OCCURENCES
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.CountOccurencesSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountOccurencesSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | categorical | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountOccurencesSelect.java) | selection | list of `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### COUNT_QUARTERS
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.CountQuartersSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountQuartersSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | categorical | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### DATE_DISTANCE
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.DateDistanceSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/DateDistanceSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | categorical | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/DateDistanceSelect.java) | timeUnit | one of NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, MONTHS, YEARS, DECADES, CENTURIES, MILLENNIA, ERAS, FOREVER |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### DATE_UNION
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.DateUnionSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/DateUnionSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | categorical | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### DISTINCT
Java Type: `com.bakdata.conquery.models.concepts.select.connector.DistinctSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/DistinctSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | categorical | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### DURATION_SUM
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.DurationSumSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/DurationSumSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | categorical | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### EXISTS
Java Type: `com.bakdata.conquery.models.concepts.select.concept.specific.ExistsSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/concept/specific/ExistsSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### FIRST
Java Type: `com.bakdata.conquery.models.concepts.select.connector.FirstValueSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/FirstValueSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | categorical | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### LAST
Java Type: `com.bakdata.conquery.models.concepts.select.connector.LastValueSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/LastValueSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | categorical | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### PREFIX
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.PrefixSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/PrefixSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | categorical | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/PrefixSelect.java) | prefix | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### QUARTERS_IN_YEAR
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.QuartersInYearSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/QuartersInYearSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | categorical | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### RANDOM
Java Type: `com.bakdata.conquery.models.concepts.select.connector.RandomValueSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/RandomValueSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | categorical | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### SUM
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.SumSelect`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/Select.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect.java) | distinct | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect.java) | distinctByColumn | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect.java) | subtractColumn | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 



---

## Other Types

### Type ConceptTreeChild
Java Type: `com.bakdata.conquery.models.concepts.tree.ConceptTreeChild`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/ConceptTreeChild.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java) | additionalInfos | list of [KeyValue](#Type-KeyValue) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement.java) | description | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/ConceptTreeChild.java) | children | list of [ConceptTreeChild](#Type-ConceptTreeChild) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/tree/ConceptTreeChild.java) | condition | [CTCondition](#Base-CTCondition) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### Type Connector
Java Type: `com.bakdata.conquery.models.concepts.Connector`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Connector.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Connector.java) | selects | list of [Select](#Base-Select) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/Connector.java) | validityDates | list of [ValidityDate](#Type-ValidityDate) |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 

### Type FilterTemplate
Java Type: `com.bakdata.conquery.apiv1.FilterTemplate`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java) | columnValue | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java) | columns | list of `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java) | filePath | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java) | optionValue | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate.java) | value | `String` |  |  | 

### Type KeyValue
Java Type: `com.bakdata.conquery.models.common.KeyValue`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/common/KeyValue.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/common/KeyValue.java) | key | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/common/KeyValue.java) | value | `String` |  |  | 

### Type ValidityDate
Java Type: `com.bakdata.conquery.models.concepts.ValidityDate`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ValidityDate.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/concepts/ValidityDate.java) | column | ID of `Column` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `"someLabel"` | visible label shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` |  |  | 
