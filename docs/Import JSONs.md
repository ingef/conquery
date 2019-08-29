
# Import JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.

Each `*.import.json` has to contain exactly one [ImportDescriptor](#ImportDescriptor).


---

## Base Output


Different types of Output can be used by setting `operation` to one of the following values:


### CONCAT
Java Type: `com.bakdata.conquery.models.preproc.outputs.ConcatOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/ConcatOutput) | inputColumns | list of `int` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 

### COPY
Java Type: `com.bakdata.conquery.models.preproc.outputs.CopyOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/CopyOutput) | inputColumn | `int` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/CopyOutput) | inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 

### DATE_RANGE
Java Type: `com.bakdata.conquery.models.preproc.outputs.DateRangeOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/DateRangeOutput) | endColumn | `int` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/DateRangeOutput) | startColumn | `int` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 

### EPOCH
Java Type: `com.bakdata.conquery.models.preproc.outputs.EpochOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochOutput) | column | `int` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 

### EPOCH_DATE_RANGE
Java Type: `com.bakdata.conquery.models.preproc.outputs.EpochDateRangeOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput) | endColumn | `int` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput) | startColumn | `int` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 

### LINE
Java Type: `com.bakdata.conquery.models.preproc.outputs.LineOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 

### NULL
Java Type: `com.bakdata.conquery.models.preproc.outputs.NullOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/NullOutput) | inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 

### QUARTER_TO_FIRST_DAY
Java Type: `com.bakdata.conquery.models.preproc.outputs.QuarterToFirstDayOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/QuarterToFirstDayOutput) | quarterColumn | `int` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/QuarterToFirstDayOutput) | yearColumn | `int` | 

### QUARTER_TO_RANGE
Java Type: `com.bakdata.conquery.models.preproc.outputs.QuarterToRangeOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/QuarterToRangeOutput) | quarterColumn | `int` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/QuarterToRangeOutput) | yearColumn | `int` | 

### SOURCE
Java Type: `com.bakdata.conquery.models.preproc.outputs.SourceOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 

### STARTING_FROM
Java Type: `com.bakdata.conquery.models.preproc.outputs.StartingFromOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/StartingFromOutput) | inputColumn | `int` | 

### UNPIVOT
Java Type: `com.bakdata.conquery.models.preproc.outputs.UnpivotOutput`

The following fields are supported:

|  | Field | Type |
| --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | name | `String` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output) | required | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/UnpivotOutput) | includeNulls | `boolean` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/UnpivotOutput) | inputColumns | list of `int` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/src/main/java/com/bakdata/conquery/models/preproc/outputs/UnpivotOutput) | inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE | 



---

## Other Types
