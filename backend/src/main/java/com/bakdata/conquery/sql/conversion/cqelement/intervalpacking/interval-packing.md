# What is interval packing?

The solution for the interval packing problem was taken
from [this article](https://www.itprotoday.com/sql-server/new-solution-packing-intervals-problem).

Interval packing refers to packing groups of intersecting date intervals into their respective continuous intervals.
It allows us to aggregate the date range of events into date range sets for a given entity.

A short example: consider this SQL query where the filter criteria for subjects is `"foo" = 'bar'` and
the validity date is represented by a date range between the dates `"date_start"` and `"date_end"`.

When aggregating dates, we treat the end date of date ranges as excluded, but in our database tables, end dates are
included - that's why we add +1 day to the end date from the database table.

```sql
select "id",
       "foo",
       "date_start",
       "date_end" + 1
from "table"
where "foo" = 'bar'
```

and suppose the result set looks like this:

| id | foo | date\_start | date\_end  |
|:---|:----|:------------|:-----------|
| 1  | bar | 2013-01-02  | 2013-02-02 |
| 1  | bar | 2012-01-01  | 2012-07-02 |
| 1  | bar | 2012-06-01  | 2013-01-01 |

While the first entry is not intersecting with the two others, entry 2 and 3 overlap. We have to combine the two entries
by creating a new range from `2012-01-01` to `2013-01-01`. But how do we achieve this using SQL?

## Solution for interval packing

Taking the short example from above, we want to explain how we do this using the
[AnsiSqlIntervalPacker](./AnsiSqlIntervalPacker.java). To outline, we need to
create 3 consecutive common table expressions (CTE):

- `previous_end`: Adds the previous end date to the table
- `range_index`: Creates and adds a counter for each new range
- and `interval_complete`: Converts this information into the desired format
  The last CTE will then contain the aggregated validity date ranges for each subject.

### `previous_end`

This table generates the `previous_end` select: The `previous_end` value for each row will be the maximum `date_end`
within the same `id` partition that occurs before the current row's `date_start`.

- `partion by "id"` creates a window frame for each subject.
- `order by "date_start", "date_end"`: arranges the rows in ascending order of their start dates and if start
  dates are the same, it ensures that the event with the earlier end date is considered first within the window frame.
- `rows between unbounded preceding and preceding 1` ensures that the maximum `date_end` that occurred before
  the current row's `date_end` is calculated. Without this clause, the window function would consider all rows in the
  partition, which might include rows with end date values greater than the current row's values.

```sql
select "id",
       "date_start",
       "date_end",
       max("date_end") over (
           partion by "id" 
           order by "date_start", "date_end" 
           rows between unbounded preceding and preceding 1
           ) as "previous_end"
from "base"
```

The result of the `previous_end` query looks like this:

| id | date\_start | date\_end  | previous\_end |
|:---|:------------|:-----------|:--------------|
| 1  | 2012-01-01  | 2012-07-02 | null          |
| 1  | 2012-06-01  | 2013-01-01 | 2012-07-02    |
| 1  | 2013-01-02  | 2013-02-02 | 2013-01-01    |

The first entry for `previous_end` is null because it has no preceding row.
The following entries contain their previous row's `date_end` value as their previous end.

### `range_index`

The `previous_end` step builds the foundation to check in the `range_index` step via the corresponding select,
whether a rows `date_start` is greater than the `previous_end` date. This indicates that two adjacent validity date
ranges do not intersect. By generating the `range_index`, we build the foundation to group intersecting date ranges
together in the following step.

- Again, we create a window frame for each subject (`partion by "id"`).
- Each time, the current rows' `date_start` > `previous_end` this row is marked with a `1`.
- We calculate the `sum` for all rows from the beginning of the partition up to the current
  row (`rows unbounded preceding`).
- Each time a row is not intersecting with the previous range, the `range_index` is increased by `1`.
- If the current rows' `date_start` <= `previous_end`, thus the rows' validity date is intersecting
  with the previous rows one, the current `range_index` value is kept.

```sql
select "id",
       "date_start",
       "date_end",
       sum(
       case
               when "date_start" > "previous_end" then 1
               else null
           end
           ) over (
           partition by "id"
           order by "date_start", "date_end"
           rows unbounded preceding
           ) "range_index"
from "previous_end")
```

The result of `range_index` looks like this:

| id | date\_start | date\_end  | range\_index |
|:---|:------------|:-----------|:-------------|
| 1  | 2012-01-01  | 2012-07-02 | null         |
| 1  | 2012-06-01  | 2013-01-01 | null         |
| 1  | 2013-01-02  | 2013-02-02 | 1            |

The first two entries intersect, because the `date_start` `2012-06-01` of the second entry is less or equal
the `previous_end` `2012-07-02` of the first entry. In contrast, the `date_start` `2013-01-02` of the third entry
starts after the `previous_end` `2013-01-01` of the second entry. Thus, it marks the beginning of a new,
non-intersecting range.

### `interval_complete`

The last step will select the minimum `date_start` and maximum `date_end` of the entries of
`range_index` table grouped by `range_index`. This ensures that there will be only unique date range values in the final
date range set of each subject.

```sql
select "id",
       min("date_start") "range_start_min",
       max("date_end")   "range_end_max"
from "range_index"
group by "id", "range_index"
```

The results look like this:

| id | range\_start\_min | range\_end\_max |
|:---|:------------------|:----------------|
| 1  | 2013-01-02        | 2013-02-02      |
| 1  | 2012-01-01        | 2013-01-01      |

In the final interval packing result, we now got 2 validity date ranges. If you compare it to our "base" table,
we now got the combined validity range from `2012-01-01` to `2013-01-01` as first entry and the succeeding
non-intersecting validity date range from `2013-01-02` to `2013-02-02` as second entry. Remember that we added +1 day
to all end dates at the beginning, so the end dates of our interval-packed ranges are excluded!

### Combined

```sql
with "base" as
         (select "id",
                 "foo",
                 "date_start",
                 "date_end" + 1
          from "table"
          where "foo" = 'bar'),
     "previous_end" as
         (select "id",
                 "date_start",
                 "date_end",
                 max("date_end") over (
                     partition by "id"
                     order by "date_start", "date_end"
                     rows between unbounded preceding and 1 preceding
                     ) as "previous_end"
          from "base"),
     "range_index" as
         (select "id",
                 "date_start",
                 "date_end",
                 sum(case
                         when "date_start" > "previous_end" then 1
                         else null
                     end) over (
                     partition by "id"
                     order by "date_start", "date_end"
                     rows unbounded preceding
                     ) "range_index"
          from "previous_end")
select "id",
       min("date_start") "range_start_min",
       max("date_end")   "range_end_max"
from "range_index"
group by "id", "range_index";
```
