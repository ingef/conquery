
# Concept JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.


---

## Base Concept


Different types of Concept can be used by setting `type` to one of the following values:


### TREE
Java Name: `com.bakdata.conquery.models.concepts.tree.TreeConcept`

The following fields are supported:

| Field | Type |
| --- | --- |
| connectors | list of `Connector` | 
| dataset | ID of `Dataset` | 
| hidden | `boolean` | 
| additionalInfos | list of `KeyValue` | 
| description | `String` | 
| children | list of `ConceptTreeChild` | 
| globalToLocalOffset | `int` | 
| selects | list of `UniversalSelect` | 
| label | `String` | 
| name | `String` | 

### VIRTUAL
Java Name: `com.bakdata.conquery.models.concepts.virtual.VirtualConcept`

The following fields are supported:

| Field | Type |
| --- | --- |
| connectors | list of `Connector` | 
| dataset | ID of `Dataset` | 
| hidden | `boolean` | 
| additionalInfos | list of `KeyValue` | 
| description | `String` | 
| selects | list of [Select](#Base-Select) | 
| label | `String` | 
| name | `String` | 



---

## Base CTCondition
a condition in a Concept

Different types of CTCondition can be used by setting `type` to one of the following values:


### AND
Java Name: `com.bakdata.conquery.models.concepts.conditions.AndCondition`

The following fields are supported:

| Field | Type |
| --- | --- |
| conditions | list of [CTCondition](#Base-CTCondition) | 

### COLUMN_EQUAL
Java Name: `com.bakdata.conquery.models.concepts.conditions.ColumnEqualCondition`

The following fields are supported:

| Field | Type |
| --- | --- |
| column | `String` | 
| values | list of `String` | 

### EQUAL
Java Name: `com.bakdata.conquery.models.concepts.conditions.EqualCondition`

The following fields are supported:

| Field | Type |
| --- | --- |
| values | list of `String` | 

### GROOVY
Java Name: `com.bakdata.conquery.models.concepts.conditions.GroovyCondition`

The following fields are supported:

| Field | Type |
| --- | --- |
| script | `String` | 

### NOT
Java Name: `com.bakdata.conquery.models.concepts.conditions.NotCondition`

The following fields are supported:

| Field | Type |
| --- | --- |
| condition | [CTCondition](#Base-CTCondition) | 

### OR
Java Name: `com.bakdata.conquery.models.concepts.conditions.OrCondition`

The following fields are supported:

| Field | Type |
| --- | --- |
| conditions | list of [CTCondition](#Base-CTCondition) | 

### PREFIX_LIST
Java Name: `com.bakdata.conquery.models.concepts.conditions.PrefixCondition`

The following fields are supported:

| Field | Type |
| --- | --- |
| prefixes | list of `String` | 

### PREFIX_RANGE
Java Name: `com.bakdata.conquery.models.concepts.conditions.PrefixRangeCondition`

The following fields are supported:

| Field | Type |
| --- | --- |
| max | `String` | 
| min | `String` | 



---

## Base Filter


Different types of Filter can be used by setting `type` to one of the following values:


### BIG_MULTI_SELECT
Java Name: `com.bakdata.conquery.models.concepts.filters.specific.BigMultiSelectFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| allowDropFile | boolean or null | 
| description | `String` | 
| pattern | `String` | 
| template | `FilterTemplate` | 
| unit | `String` | 
| column | ID of `Column` | 
| labels | `BiMap<String, String>` | 
| values | list of `String` | 
| label | `String` | 
| name | `String` | 

### COUNT
Java Name: `com.bakdata.conquery.models.concepts.filters.specific.CountFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| allowDropFile | boolean or null | 
| description | `String` | 
| pattern | `String` | 
| template | `FilterTemplate` | 
| unit | `String` | 
| column | ID of `Column` | 
| distinct | `boolean` | 
| distinctByColumn | list of ID of list of `Column` | 
| label | `String` | 
| name | `String` | 

### COUNT_QUARTERS
Java Name: `com.bakdata.conquery.models.concepts.filters.specific.CountQuartersFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| allowDropFile | boolean or null | 
| description | `String` | 
| pattern | `String` | 
| template | `FilterTemplate` | 
| unit | `String` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### DATE_DISTANCE
Java Name: `com.bakdata.conquery.models.concepts.filters.specific.DateDistanceFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| allowDropFile | boolean or null | 
| description | `String` | 
| pattern | `String` | 
| template | `FilterTemplate` | 
| unit | `String` | 
| column | ID of `Column` | 
| timeUnit | one of NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, MONTHS, YEARS, DECADES, CENTURIES, MILLENNIA, ERAS, FOREVER | 
| label | `String` | 
| name | `String` | 

### DURATION_SUM
Java Name: `com.bakdata.conquery.models.concepts.filters.specific.DurationSumFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| allowDropFile | boolean or null | 
| description | `String` | 
| pattern | `String` | 
| template | `FilterTemplate` | 
| unit | `String` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### NUMBER
Java Name: `com.bakdata.conquery.models.concepts.filters.specific.NumberFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| allowDropFile | boolean or null | 
| description | `String` | 
| pattern | `String` | 
| template | `FilterTemplate` | 
| unit | `String` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### PREFIX_TEXT
Java Name: `com.bakdata.conquery.models.concepts.filters.specific.PrefixTextFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| allowDropFile | boolean or null | 
| description | `String` | 
| pattern | `String` | 
| template | `FilterTemplate` | 
| unit | `String` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### QUARTERS_IN_YEAR
Java Name: `com.bakdata.conquery.models.concepts.filters.specific.QuartersInYearFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| allowDropFile | boolean or null | 
| description | `String` | 
| pattern | `String` | 
| template | `FilterTemplate` | 
| unit | `String` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### SELECT
Java Name: `com.bakdata.conquery.models.concepts.filters.specific.MultiSelectFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| allowDropFile | boolean or null | 
| description | `String` | 
| pattern | `String` | 
| template | `FilterTemplate` | 
| unit | `String` | 
| column | ID of `Column` | 
| labels | `BiMap<String, String>` | 
| values | list of `String` | 
| label | `String` | 
| name | `String` | 

### SINGLE_SELECT
Java Name: `com.bakdata.conquery.models.concepts.filters.specific.SelectFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| allowDropFile | boolean or null | 
| description | `String` | 
| pattern | `String` | 
| template | `FilterTemplate` | 
| unit | `String` | 
| column | ID of `Column` | 
| labels | `BiMap<String, String>` | 
| values | list of `String` | 
| label | `String` | 
| name | `String` | 

### SUM
Java Name: `com.bakdata.conquery.models.concepts.filters.specific.SumFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| allowDropFile | boolean or null | 
| description | `String` | 
| pattern | `String` | 
| template | `FilterTemplate` | 
| unit | `String` | 
| column | ID of `Column` | 
| distinct | `boolean` | 
| distinctByColumn | ID of `Column` | 
| subtractColumn | ID of `Column` | 
| label | `String` | 
| name | `String` | 



---

## Base Select
used to define selects that can be used to create additional CSV columns

Different types of Select can be used by setting `type` to one of the following values:


### COUNT
Java Name: `com.bakdata.conquery.models.concepts.select.connector.specific.CountSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| column | ID of `Column` | 
| distinct | `boolean` | 
| distinctByColumn | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### COUNT_OCCURENCES
Java Name: `com.bakdata.conquery.models.concepts.select.connector.specific.CountOccurencesSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| categorical | `boolean` | 
| column | ID of `Column` | 
| selection | list of `String` | 
| label | `String` | 
| name | `String` | 

### COUNT_QUARTERS
Java Name: `com.bakdata.conquery.models.concepts.select.connector.specific.CountQuartersSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| categorical | `boolean` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### DATE_DISTANCE
Java Name: `com.bakdata.conquery.models.concepts.select.connector.specific.DateDistanceSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| categorical | `boolean` | 
| column | ID of `Column` | 
| timeUnit | one of NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, MONTHS, YEARS, DECADES, CENTURIES, MILLENNIA, ERAS, FOREVER | 
| label | `String` | 
| name | `String` | 

### DATE_UNION
Java Name: `com.bakdata.conquery.models.concepts.select.connector.specific.DateUnionSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| categorical | `boolean` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### DISTINCT
Java Name: `com.bakdata.conquery.models.concepts.select.connector.DistinctSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| categorical | `boolean` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### DURATION_SUM
Java Name: `com.bakdata.conquery.models.concepts.select.connector.specific.DurationSumSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| categorical | `boolean` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### EXISTS
Java Name: `com.bakdata.conquery.models.concepts.select.concept.specific.ExistsSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| label | `String` | 
| name | `String` | 

### FIRST
Java Name: `com.bakdata.conquery.models.concepts.select.connector.FirstValueSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| categorical | `boolean` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### LAST
Java Name: `com.bakdata.conquery.models.concepts.select.connector.LastValueSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| categorical | `boolean` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### PREFIX
Java Name: `com.bakdata.conquery.models.concepts.select.connector.specific.PrefixSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| categorical | `boolean` | 
| column | ID of `Column` | 
| prefix | `String` | 
| label | `String` | 
| name | `String` | 

### QUARTERS_IN_YEAR
Java Name: `com.bakdata.conquery.models.concepts.select.connector.specific.QuartersInYearSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| categorical | `boolean` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### RANDOM
Java Name: `com.bakdata.conquery.models.concepts.select.connector.RandomValueSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| categorical | `boolean` | 
| column | ID of `Column` | 
| label | `String` | 
| name | `String` | 

### SUM
Java Name: `com.bakdata.conquery.models.concepts.select.connector.specific.SumSelect`

The following fields are supported:

| Field | Type |
| --- | --- |
| description | `String` | 
| column | ID of `Column` | 
| distinct | `boolean` | 
| distinctByColumn | ID of `Column` | 
| subtractColumn | ID of `Column` | 
| label | `String` | 
| name | `String` | 


