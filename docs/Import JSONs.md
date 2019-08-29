
# Import JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.

Each `*.import.json` has to contain exactly one [ImportDescriptor](#ImportDescriptor).


---

## Base Output


Different types of Output can be used by setting `operation` to one of the following values:


### CONCAT
Java Type: `com.bakdata.conquery.models.preproc.outputs.ConcatOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/ConcatOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/ConcatOutput.java) | inputColumns | list of `int` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 

### COPY
Java Type: `com.bakdata.conquery.models.preproc.outputs.CopyOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/CopyOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/CopyOutput.java) | inputColumn | `int` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/CopyOutput.java) | inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 

### DATE_RANGE
Java Type: `com.bakdata.conquery.models.preproc.outputs.DateRangeOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/DateRangeOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/DateRangeOutput.java) | endColumn | `int` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/DateRangeOutput.java) | startColumn | `int` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 

### EPOCH
Java Type: `com.bakdata.conquery.models.preproc.outputs.EpochOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochOutput.java) | column | `int` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 

### EPOCH_DATE_RANGE
Java Type: `com.bakdata.conquery.models.preproc.outputs.EpochDateRangeOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput.java) | endColumn | `int` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput.java) | startColumn | `int` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 

### LINE
Java Type: `com.bakdata.conquery.models.preproc.outputs.LineOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/LineOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 

### NULL
Java Type: `com.bakdata.conquery.models.preproc.outputs.NullOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/NullOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/NullOutput.java) | inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 

### QUARTER_TO_FIRST_DAY
Java Type: `com.bakdata.conquery.models.preproc.outputs.QuarterToFirstDayOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/QuarterToFirstDayOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/QuarterToFirstDayOutput.java) | quarterColumn | `int` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/QuarterToFirstDayOutput.java) | yearColumn | `int` |  |  | 

### QUARTER_TO_RANGE
Java Type: `com.bakdata.conquery.models.preproc.outputs.QuarterToRangeOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/QuarterToRangeOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/QuarterToRangeOutput.java) | quarterColumn | `int` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/QuarterToRangeOutput.java) | yearColumn | `int` |  |  | 

### SOURCE
Java Type: `com.bakdata.conquery.models.preproc.outputs.SourceOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/SourceOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 

### STARTING_FROM
Java Type: `com.bakdata.conquery.models.preproc.outputs.StartingFromOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/StartingFromOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/StartingFromOutput.java) | inputColumn | `int` |  |  | 

### UNPIVOT
Java Type: `com.bakdata.conquery.models.preproc.outputs.UnpivotOutput`

[✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/UnpivotOutput.java) 

The following fields are supported:

|  | Field | Type | Example | Description |
| --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | name | `String` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/Output.java) | required | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/UnpivotOutput.java) | includeNulls | `boolean` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/UnpivotOutput.java) | inputColumns | list of `int` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/UnpivotOutput.java) | inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE |  |  | 



---

## Other Types

---

## Marker Interfaces
