# Form conversion - how to apply stratification in SQL?

This document outlines the procedure to apply stratification within SQL in the context of the form conversion process.
The process or to be more precise, specific functions used while creating stratification tables differ from dialect
to dialect, but the overall process is the same. This document is using Postgres dialect for the SQL examples.

# Table of Contents

1. [Prerequisite conversion](#prerequisite-conversion)
2. [Absolute stratification](#absolute-stratification-for-absolute-forms-and-entity-date-forms)
    1. [For absolute forms](#absolute-forms)
    2. [For entity date queries](#entity-date)
    3. [Stratification tables](#stratification-tables)
3. [Feature conversion](#feature-conversion)
4. [Left-join converted features](#left-join-converted-features-with-the-full-stratification-table-for-the-final-select)

## Prerequisite conversion

The prerequisite query conversion produces a CTE, which will contain the IDs of those subjects relevant for the form.
Because this could be any kind of Query, the CTE might also contain a validity date and converted Selects.
Take this CTE representing a converted CQExternal as an example:

**CTE:** `external`

```sql
select '1'                                            "primary_id",
       daterange('2001-12-01', '2016-12-02', '[)') as "validity_date"
from "DUMMY"; -- "DUMMY" is SAP HANAs built-in no-op table
```

## Absolute stratification (for absolute forms and entity date forms)

### Absolute forms

For an absolute form, we only care for the primary ID, so we extract the primary IDs and discard all other fields from 
the prerequisite query. The `stratification_bounds` represent the absolute forms date range. They define the required 
complete stratification window. We group by primary ID to keep only distinct entries for each entity and discard any 
duplicated entries which, for example, might occur due to a preceding secondary id query.

**CTE:** `extract_ids`

```sql
select "primary_id",
       daterange('2012-06-01', '2012-09-30', '[]') as "stratification_bounds"
from "external"
group by "primary_id"
```

### Entity date

For an entity date form, we only care for the primary ID and the validity date. We group by primary ID and validity
date to keep only distinct entries for each entity and discard any duplicated entries which, for example, might occur
due to a preceding secondary id query.

**CTE:** `extract_ids`

```sql
select "primary_id",
       "validity_date"
from "external"
group by "primary_id", "validity_date"
```

**CTE:** `overwrite_bounds`

We create an additional CTE which intersects the entities validity dates with the given forms date range (if there is
one). The intersection defines the required complete stratification window. Besides this, there is no difference in the
following conversion process between absolute forms and entity date queries.

```sql
select "primary_id",
       -- the validity date is a multirange, so we unnest first 
       daterange('2012-06-01', '2012-09-30', '[]') * unnest("validity_date") as "stratification_bounds"
from "extract_ids"
```

### Stratification tables

Now, we want to create a resolution table for each resolution (`COMPLETE`, `YEAR`, `QUARTER`).

**CTE:** `complete`

```sql
select "primary_id",
       'COMPLETE'                          "resolution",
       1 "index",
       "stratification_bounds"
from "extract_ids" -- or `overwrite_bounds` if it is an entity date query
```

A complete range shall have a `null` index, because it spans the complete range, but we set it to 1 to ensure we can
join tables on index.

### Calculating index start dates

For finer resolutions (`YEAR`, `QUARTER`, `DAY`), the approach will be the following: at first, we will calculate
the required year and quarter start and end dates for all possible resolution and alignment combos. We will use these
later to calculate date distances easily.

We will create the following fields:

- `year_start`: This field is calculated by truncating the lower boundary of `stratification_bounds` to the start of the
  corresponding year, providing the earliest date of the year in which the stratification begins.
- `quarter_start`: This field is derived by truncating the lower boundary of `stratification_bounds` to the start of its
  year and then adjusting for the number of complete quarters passed since the beginning of that year, marking the start
  of the quarter in which the stratification starts.
- `year_end`: This field captures the last day of the year for the upper boundary of `stratification_bounds`. It is
  calculated by truncating the upper boundary to the start of its year, advancing to the start of the following year,
  and then subtracting one day, thus marking the end of the year in which the stratification ends.
- `year_end_quarter_aligned`: This field aligns the end of a calendar year of the upper boundary
  of `stratification_bounds`
  with the quarter of the starting date. This edge case is required for the `YEAR` resolution, `QUARTER` alignment
  combo.
- `quarter_end`: This field indicates the last day of the quarter for the upper boundary of `stratification_bounds`. It
  is calculated by truncating the upper boundary to the start of its year, advancing to the start of the next quarter
  after the quarter in which the stratification ends, and subtracting one day to find the close of that quarter.

```sql
select "primary_id",
       "stratification_bounds",
       lower("stratification_bounds")                                         as "index_start",
       date_trunc('year', lower("stratification_bounds"))                     as "year_start",
       date_trunc('year', upper("stratification_bounds")) + interval '1 year' as "year_end",
       date_trunc('year', upper("stratification_bounds")) + interval '1 year'
           + (extract(quarter from lower("stratification_bounds")) - 1)
           * interval '3 months'                                              as "year_end_quarter_aligned",
       date_trunc('year', lower("stratification_bounds"))
           + (extract(quarter from lower("stratification_bounds")) - 1)
           * interval '3 months'                                              as "quarter_start",
       date_trunc('year', upper("stratification_bounds"))
           + (extract(quarter from upper("stratification_bounds")))
           * interval '3 months'                                              as "quarter_end"
from "extract_ids"
```

| primary\_id | stratification\_bounds    | index\_start | year\_start | year\_end  | year\_end\_quarter\_aligned | quarter\_start | quarter\_end | 
|:------------|:--------------------------|:-------------|:------------|:-----------|:----------------------------|:---------------|:-------------|
| 1           | \[2012-01-16,2012-12-18\) | 2012-01-16   | 2012-01-01  | 2013-01-01 | 2013-01-01                  | 2012-01-01     | 2013-01-01   |

### Calculating resolution counts

For each required resolution and alignment, we will create a `counts` CTE. The calculation slightly differs for each
valid resolution and alignment combination, but it all comes down to calculating date diffs.

For example, take the `YEAR` resolution and `QUARTER` alignment as an example. We calculate the date diff in years from
the quarter end (of the upper stratification bound) and the quarter start (of the lower stratification bound) that we
calculated in the previous step.

```sql
select "primary_id",
       "stratification_bounds",
       "index_start",
       "year_start",
       "quarter_start",
       (extract(year from age("quarter_end", "quarter_start"))) as "quarter_aligned_count"
from "index_start";
```

For this example, the `"year_aligned_count"` is 3.

### Calculating the actual stratification ranges

The key idea of the resolution count is to calculate a start and end date for all required resolution windows by adding
a date interval times an index until the index reaches the count. For example, if we consider the `quarter_start`
`2012-04-01` as out starting point, the resolution interval to be 1 year and the resolution count to be 3, we can
calculate the resolution range with the following approach:

```sql
select "primary_id",
       'YEARS'                                       as "resolution",
       row_number() over (partition by "primary_id") as "index",
       daterange(
               ("quarter_start" + interval '1 year' * ("index" - 1))::date,
               ("quarter_start" + interval '1 year' * ("index" - 0))::date,
               '[)'
       ) * "stratification_bounds"                   as "stratification_bounds"
from "year_counts",
     generate_series(1, 10000) as "index"
where "index" <= "quarter_aligned_count"
```

By cross-joining the `year_counts` CTE with the integer series, we can create a set of ranges for each primary ID.
Intervals are multiplied and added depending on the respective index to the starting date, which is the `quarter_start`
in our case. The result looks like this:

| primary\_id | resolution | index | stratification\_bounds    |
|:------------|:-----------|:------|:--------------------------|
| 1           | YEARS      | 1     | \[2012-06-16,2013-04-01\) |
| 1           | YEARS      | 2     | \[2013-04-01,2014-04-01\) |
| 1           | YEARS      | 3     | \[2014-04-01,2014-12-18\) |

**CTE:** `full_stratification`

Now, we union all the resolution tables.

```sql
select "complete"."primary_id",
       "complete"."resolution",
       "complete"."index",
       "complete"."stratification_bounds"
from "complete"
union all
select "years"."primary_id",
       "years"."resolution",
       "years"."index",
       "years"."stratification_bounds"
from "years"
union all
select "quarters"."primary_id",
       "quarters"."resolution",
       "quarters"."index",
       "quarters"."stratification_bounds"
from "quarters"
```

| primary\_id | resolution | index | stratification\_bounds    |
|:------------|:-----------|:------|:--------------------------|
| 1           | COMPLETE   | 1     | \[2012-06-16,2014-12-18\) |
| 1           | YEARS      | 1     | \[2012-06-16,2013-04-01\) |
| 1           | YEARS      | 2     | \[2013-04-01,2014-04-01\) |
| 1           | YEARS      | 3     | \[2014-04-01,2014-12-18\) |
| 1           | QUARTERS   | 1     | \[2012-06-16,2012-07-01\) |
| 1           | QUARTERS   | 2     | \[2012-07-01,2012-10-01\) |
| 1           | QUARTERS   | 3     | \[2012-10-01,2013-01-01\) |
| 1           | QUARTERS   | 4     | \[2013-01-01,2013-04-01\) |
| 1           | QUARTERS   | 5     | \[2013-04-01,2013-07-01\) |
| 1           | QUARTERS   | 6     | \[2013-07-01,2013-10-01\) |
| 1           | QUARTERS   | 7     | \[2013-10-01,2014-01-01\) |
| 1           | QUARTERS   | 8     | \[2014-01-01,2014-04-01\) |
| 1           | QUARTERS   | 9     | \[2014-04-01,2014-07-01\) |
| 1           | QUARTERS   | 10    | \[2014-07-01,2014-10-01\) |
| 1           | QUARTERS   | 11    | \[2014-10-01,2014-12-18\) |

## Feature conversion

After we got our full stratification table, containing all stratification windows for each ID, we want to convert all
the features of the form, while using our stratification table as a starting point:

1. When converting a concept and creating the `PREPROCESSING` CTE, which is the starting point of each concept
   conversion, we join the concepts or respectively the connectors table with the stratification table for all IDs from
   the stratification table.

**CTE:** `preprocessing`

```sql
select "full_stratification"."primary_id",
       "full_stratification"."resolution",
       "full_stratification"."index",
       "full_stratification"."stratification_range_start",
       "full_stratification"."stratification_range_end",
       "vers_stamm"."date_start"            "validity_date_start",
       ADD_DAYS("vers_stamm"."date_end", 1) "validity_date_end",
       "vers_stamm"."date_of_birth"
from "vers_stamm"
         join "full_stratification"
              on "full_stratification"."primary_id" = "vers_stamm"."pid"
```

2. In the `EVENT_FILTER` step, we filter all entries where the stratification range and the subjects validity date do
   not overlap. This is important because we only want to compute aggregations for those ranges that satisfy this
   condition.

**CTE:** `event_filter`

```sql
select "primary_id",
       "resolution",
       "index",
       "stratification_range_start",
       "stratification_range_end",
       "validity_date_start",
       "validity_date_end",
       "date_of_birth"
from "preprocessing"
where "stratification_range_start" < "validity_date_end"
  and "stratification_range_end" > "validity_date_start"
```

Besides grouping by ID, resolution, index and stratification range, the remaining concept conversion CTE process
remains as usual. If we have multiple features, we'll join the respective converted concept queries via an OR.

## Left-join converted features with the full stratification table for the final select

For an absolute form, we expect the final result to contain all stratification ranges for each ID of the respective
chosen resolutions. Because we filter all entries where stratification range and validity date do not overlap in each
concept conversion's event filter step, the converted feature(s) table might not contain all stratification ranges.
Thus, we left-join the table with the converted feature(s) back with the full stratification table. 
