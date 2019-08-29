
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

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/Concept) | connectors | list of [Connector](#Type-Connector) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/Concept) | dataset | ID of `Dataset` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/Concept) | hidden | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement) | additionalInfos | list of [KeyValue](#Type-KeyValue) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/tree/TreeConcept) | children | list of [ConceptTreeChild](#Type-ConceptTreeChild) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/tree/TreeConcept) | globalToLocalOffset | `int` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/tree/TreeConcept) | selects | list of `UniversalSelect` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### VIRTUAL
Java Type: `com.bakdata.conquery.models.concepts.virtual.VirtualConcept`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/Concept) | connectors | list of [Connector](#Type-Connector) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/Concept) | dataset | ID of `Dataset` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/Concept) | hidden | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement) | additionalInfos | list of [KeyValue](#Type-KeyValue) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/virtual/VirtualConcept) | selects | list of [Select](#Base-Select) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 



---

## Base CTCondition
These represent guard conditions. A value matches a [ConceptElement](#ConceptElement) if it matches its condition and its parent

Different types of CTCondition can be used by setting `type` to one of the following values:


### AND
Java Type: `com.bakdata.conquery.models.concepts.conditions.AndCondition`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/conditions/AndCondition) | conditions | list of [CTCondition](#Base-CTCondition) | 

### COLUMN_EQUAL
Java Type: `com.bakdata.conquery.models.concepts.conditions.ColumnEqualCondition`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/conditions/ColumnEqualCondition) | column | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/conditions/ColumnEqualCondition) | values | list of `String` | 

### EQUAL
Java Type: `com.bakdata.conquery.models.concepts.conditions.EqualCondition`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/conditions/EqualCondition) | values | list of `String` | 

### GROOVY
Java Type: `com.bakdata.conquery.models.concepts.conditions.GroovyCondition`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/conditions/GroovyCondition) | script | `String` | 

### NOT
Java Type: `com.bakdata.conquery.models.concepts.conditions.NotCondition`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/conditions/NotCondition) | condition | [CTCondition](#Base-CTCondition) | 

### OR
Java Type: `com.bakdata.conquery.models.concepts.conditions.OrCondition`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/conditions/OrCondition) | conditions | list of [CTCondition](#Base-CTCondition) | 

### PREFIX_LIST
Java Type: `com.bakdata.conquery.models.concepts.conditions.PrefixCondition`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixCondition) | prefixes | list of `String` | 

### PREFIX_RANGE
Java Type: `com.bakdata.conquery.models.concepts.conditions.PrefixRangeCondition`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixRangeCondition) | max | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/conditions/PrefixRangeCondition) | min | `String` | 



---

## Base Filter
These are used to define filters, than can be used to reduce the result set.

Different types of Filter can be used by setting `type` to one of the following values:


### BIG_MULTI_SELECT
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.BigMultiSelectFilter`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | allowDropFile | boolean or null | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | pattern | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | template | [FilterTemplate](#Type-FilterTemplate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | unit | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter) | labels | bijective map from `String` to `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter) | values | list of `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### COUNT
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.CountFilter`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | allowDropFile | boolean or null | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | pattern | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | template | [FilterTemplate](#Type-FilterTemplate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | unit | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountFilter) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountFilter) | distinct | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/CountFilter) | distinctByColumn | list of ID of list of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### COUNT_QUARTERS
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.CountQuartersFilter`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | allowDropFile | boolean or null | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | pattern | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | template | [FilterTemplate](#Type-FilterTemplate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | unit | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### DATE_DISTANCE
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.DateDistanceFilter`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | allowDropFile | boolean or null | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | pattern | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | template | [FilterTemplate](#Type-FilterTemplate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | unit | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/DateDistanceFilter) | timeUnit | one of NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, MONTHS, YEARS, DECADES, CENTURIES, MILLENNIA, ERAS, FOREVER | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### DURATION_SUM
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.DurationSumFilter`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | allowDropFile | boolean or null | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | pattern | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | template | [FilterTemplate](#Type-FilterTemplate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | unit | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### NUMBER
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.NumberFilter`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | allowDropFile | boolean or null | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | pattern | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | template | [FilterTemplate](#Type-FilterTemplate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | unit | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### PREFIX_TEXT
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.PrefixTextFilter`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | allowDropFile | boolean or null | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | pattern | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | template | [FilterTemplate](#Type-FilterTemplate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | unit | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### QUARTERS_IN_YEAR
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.QuartersInYearFilter`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | allowDropFile | boolean or null | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | pattern | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | template | [FilterTemplate](#Type-FilterTemplate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | unit | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### SELECT
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.MultiSelectFilter`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | allowDropFile | boolean or null | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | pattern | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | template | [FilterTemplate](#Type-FilterTemplate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | unit | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter) | labels | bijective map from `String` to `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter) | values | list of `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### SINGLE_SELECT
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.SelectFilter`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | allowDropFile | boolean or null | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | pattern | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | template | [FilterTemplate](#Type-FilterTemplate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | unit | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/SingleColumnFilter) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter) | labels | bijective map from `String` to `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/AbstractSelectFilter) | values | list of `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### SUM
Java Type: `com.bakdata.conquery.models.concepts.filters.specific.SumFilter`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | allowDropFile | boolean or null | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | pattern | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | template | [FilterTemplate](#Type-FilterTemplate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/Filter) | unit | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter) | distinct | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter) | distinctByColumn | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/filters/specific/SumFilter) | subtractColumn | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 



---

## Base Select
These are used to define selects, that can be used to create additional CSV columns.

Different types of Select can be used by setting `type` to one of the following values:


### COUNT
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.CountSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountSelect) | distinct | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountSelect) | distinctByColumn | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### COUNT_OCCURENCES
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.CountOccurencesSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | categorical | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/CountOccurencesSelect) | selection | list of `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### COUNT_QUARTERS
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.CountQuartersSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | categorical | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### DATE_DISTANCE
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.DateDistanceSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | categorical | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/DateDistanceSelect) | timeUnit | one of NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, MONTHS, YEARS, DECADES, CENTURIES, MILLENNIA, ERAS, FOREVER | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### DATE_UNION
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.DateUnionSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | categorical | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### DISTINCT
Java Type: `com.bakdata.conquery.models.concepts.select.connector.DistinctSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | categorical | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### DURATION_SUM
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.DurationSumSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | categorical | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### EXISTS
Java Type: `com.bakdata.conquery.models.concepts.select.concept.specific.ExistsSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### FIRST
Java Type: `com.bakdata.conquery.models.concepts.select.connector.FirstValueSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | categorical | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### LAST
Java Type: `com.bakdata.conquery.models.concepts.select.connector.LastValueSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | categorical | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### PREFIX
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.PrefixSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | categorical | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/PrefixSelect) | prefix | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### QUARTERS_IN_YEAR
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.QuartersInYearSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | categorical | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### RANDOM
Java Type: `com.bakdata.conquery.models.concepts.select.connector.RandomValueSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | categorical | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/SingleColumnSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### SUM
Java Type: `com.bakdata.conquery.models.concepts.select.connector.specific.SumSelect`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/Select) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect) | distinct | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect) | distinctByColumn | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/select/connector/specific/SumSelect) | subtractColumn | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 



---

## Other Types

### ConceptTreeChild
Java Type: `com.bakdata.conquery.models.concepts.tree.ConceptTreeChild`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement) | additionalInfos | list of [KeyValue](#Type-KeyValue) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/ConceptElement) | description | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/tree/ConceptTreeChild) | children | list of [ConceptTreeChild](#Type-ConceptTreeChild) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/tree/ConceptTreeChild) | condition | [CTCondition](#Base-CTCondition) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### Connector
Java Type: `com.bakdata.conquery.models.concepts.Connector`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/Connector) | selects | list of [Select](#Base-Select) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/Connector) | validityDates | list of [ValidityDate](#Type-ValidityDate) | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 

### FilterTemplate
Java Type: `com.bakdata.conquery.apiv1.FilterTemplate`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate) | columnValue | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate) | columns | list of `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate) | filePath | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate) | optionValue | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/apiv1/FilterTemplate) | value | `String` | 

### KeyValue
Java Type: `com.bakdata.conquery.models.common.KeyValue`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/common/KeyValue) | key | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/common/KeyValue) | value | `String` | 

### ValidityDate
Java Type: `com.bakdata.conquery.models.concepts.ValidityDate`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/concepts/ValidityDate) | column | ID of `Column` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/Labeled) | label | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl) | name | `String` | 
