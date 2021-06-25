
# Table JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.

Each `*.table.json` has to contain exactly one [Tabel](#Type-Tabel).


---

## Other Types

### Type Column<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/datasets/Column.java#L28)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.datasets.Column`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/datasets/Column.java#L42) | position | `int` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/datasets/Column.java#L49-L52) | secondaryId | ID of `@NsIdRef SecondaryIdDescription` | `null` |  | if this is set this column counts as the secondary id of the given name for this table | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/datasets/Column.java#L44-L47) | sharedDictionary | `String` | `null` |  | if set this column should use the given dictionary if it is of type string, instead of its own dictionary | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/datasets/Column.java#L39) | type | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L25-L29) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L17) | name | `String` | `null` |  |  | 
</p></details>

### Type Table<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/datasets/Table.java#L23)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.datasets.Table`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/datasets/Table.java#L33) | columns | list of [Column](#Type-Column) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/datasets/Table.java#L30) | dataset | ID of `@NonNull Dataset` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L25-L29) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L17) | name | `String` | `null` |  |  | 
</p></details>
