# Form conversion - how to apply stratification in SQL?

This document outlines the procedure to apply stratification within SQL in the context of the form conversion process.

## Prerequisite conversion

The prerequisite query conversion produces a CTE, which will contain the IDs of those subjects relevant for the form.
Because this could be any kind of Query, the CTE might also contain a validity date and converted Selects.
Take this CTE representing a converted CQExternal as an example:

**CTE:** `external`

```sql
select '1'                                 "primary_id",
       TO_DATE('2001-12-01', 'yyyy-mm-dd') "dates_start",
       TO_DATE('2016-12-02', 'yyyy-mm-dd') "dates_end"
from "DUMMY"; -- "DUMMY" is SAP HANAs built-in no-op table
```

## Absolute stratification

This example is covering the following
testcase: `src/test/resources/tests/form/EXPORT_FORM/ABSOLUT/SIMPLE/ABS_EXPORT_FORM.test.json`.

For an absolute form, we only care for the primary ID, so we extract the primary IDs (and discard the validity date).
We group by primary ID to keep only 1 entry per subject (a ≥`select distinct` would to the trick too).

**CTE:** `extract_ids`

```sql
select "primary_id"
from "external"
group by "primary_id"
```

Now, we want to create a resolution table for each resolution (`COMPLETE`, `YEAR`, `QUARTER`).

**CTE:** `complete`

```sql
select "primary_id",
       'COMPLETE'                          "resolution",
       1 "index",
       TO_DATE('2012-01-16', 'yyyy-mm-dd') "stratification_range_start",
       TO_DATE('2012-12-18', 'yyyy-mm-dd') "stratification_range_end"
from "extract_ids"
```

For an absolute form, the is only 1 complete range which spans over the given forms date
range `[2012-01-16,2012-12-18)`. Thus, the complete `stratification_range_start` and `stratification_range_end` are
simply the static values given by
the forms date range.

A complete range shall have a `null` index, because it spans the complete range, but we set it to 1 to ensure we can
join tables on index. We do this, because a condition involving `null` in a join (e.g., `null = some_value` or
`null = null`) always evaluates to false, which would cause incorrect joining results.

**CTE:** `years`

```sql
select "primary_id",
       'YEARS'                                                                 "resolution",
       row_number() over (partition by "primary_id")                           "index",
       greatest("GENERATED_PERIOD_START", TO_DATE('2012-01-16', 'yyyy-mm-dd')) "stratification_range_start",
       least("GENERATED_PERIOD_END", TO_DATE('2012-12-18', 'yyyy-mm-dd'))      "stratification_range_end"
from "extract_ids", SERIES_GENERATE_DATE('INTERVAL 1 YEAR', TO_DATE('2012-01-01', 'yyyy-mm-dd'),
                                         TO_DATE('2013-01-01', 'yyyy-mm-dd'))
```

For `YEAR` and `QUARTER`, we generate a series over the whole forms date range. Therefore, the forms actual range
`[2012-01-16,2012-12-18)` has to be adjusted: the start and end range do not cover the interval of 1 year, thus the
generated series would be an empty set. That's why we set the start date of the generated series to the first day of
the year of the forms date range start: for `2012-01-16`, this is the `2012-01-01`. The end date is set to the first day
of the year of the forms date range end + 1 year: for `2012-12-17`, it's the `2013-01-01`.

`SERIES_GENERATE_DATE('INTERVAL 1 YEAR', TO_DATE('2012-01-01', 'yyyy-mm-dd'), TO_DATE('2013-01-01', 'yyyy-mm-dd'))`
creates the following set:

| GENERATED\_PERIOD\_START | GENERATED\_PERIOD\_END |
|:-------------------------|:-----------------------|
| 2012-01-01               | 2013-01-01             |

For HANA, the two columns names are pre-generated - that's why we can use them directly in the select statement.

Because the generated series start might be before the forms absolute date range start and/or the generated series end
after the forms absolute date range end, we cover these edge cases by using the `greatest()` and `least()` functions to
compute the correctly bound stratification dates.

**CTE:** `quarters`

```sql
select "primary_id",
       'QUARTER'                                                               "resolution",
       row_number() over (partition by "primary_id")                           "index",
       greatest("GENERATED_PERIOD_START", TO_DATE('2012-01-16', 'yyyy-mm-dd')) "stratification_range_start",
       least("GENERATED_PERIOD_END", TO_DATE('2012-12-17', 'yyyy-mm-dd') "stratification_range_end"
             from "extract_ids",
             SERIES_GENERATE_DATE('INTERVAL 3 MONTH', TO_DATE('2012-01-01', 'yyyy-mm-dd'),
                                  TO_DATE('2013-01-01', 'yyyy-mm-dd'))
```

Similar to the `YEAR` resolution, we generate a series, but this time with a changed interval of 3 month (1 quarter).
The generated series looks like this:

| GENERATED\_PERIOD\_START | GENERATED\_PERIOD\_END |
|:-------------------------|:-----------------------|
| 2012-01-01               | 2012-04-01             |
| 2012-04-01               | 2012-07-01             |
| 2012-07-01               | 2012-10-01             |
| 2012-10-01               | 2013-01-01             |

Again, we make sure the stratification dates have the correct bounds via `greatest()` and `least()`.

**CTE:** `full_stratification`

Now, we union all the resolution tables.

```sql
select "complete"."primary_id",
       "complete"."resolution",
       "complete"."index",
       "complete"."stratification_range_start",
       "complete"."stratification_range_end"
from "complete"
union all
select "years"."primary_id",
       "years"."resolution",
       "years"."index",
       "years"."stratification_range_start",
       "years"."stratification_range_end"
from "years"
union all
select "quarters"."primary_id",
       "quarters"."resolution",
       "quarters"."index",
       "quarters"."stratification_range_start",
       "quarters"."stratification_range_end"
from "quarters"
```

| primary\_id | resolution | index | stratification\_range\_start | stratification\_range\_end |
|:------------|:-----------|:------|:-----------------------------|:---------------------------|
| 1           | COMPLETE   | 1     | 2012-01-16                   | 2012-12-18                 |
| 1           | YEARS      | 1     | 2012-01-16                   | 2012-12-18                 |
| 1           | QUARTERS   | 1     | 2012-01-16                   | 2012-04-01                 |
| 1           | QUARTERS   | 2     | 2012-04-01                   | 2012-07-01                 |
| 1           | QUARTERS   | 3     | 2012-07-01                   | 2012-10-01                 |
| 1           | QUARTERS   | 4     | 2012-10-01                   | 2012-12-18                 |

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
