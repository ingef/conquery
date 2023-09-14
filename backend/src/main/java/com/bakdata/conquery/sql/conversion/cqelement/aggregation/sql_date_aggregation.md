# TODO FINISH EXPLANATION AFTER WE KNOW HOW TO HANDLE VALUES WITH EMPTY RANGES

# SQL date aggregation

When joining multiple nodes with their respective validity date ranges, we need to aggregate these dates.
There are 2 aggregation actions we can apply:

- `MERGE`: unions the validity dates of all nodes
- `INTERSECT`: intersects the validity dates of all nodes

Besides that, there is also the possibility to `NEGATE` a date aggregation: this will invert the validity date ranges of
a node. Finally, dates of a certain node can also be `BLOCKED` which will block the upwards aggregation of a nodes'
validity dates.

The document outlines the process the [AnsiSqlDateAggregator](./AnsiSqlDateAggregator.java) employs to realise the
different aggregations.

Suppose we have two concepts, each containing a date range set with only unique date range values.
Starting point of the date aggregation process will be the joined node of the two nodes. Like for interval packing,
we treat the end date of date ranges as excluded. Thus, in the following example tables, the `date_end` is considered
excluded.

**Node 1**:

| id | date\_start | date\_end  |
|:---|:------------|:-----------|
| 1  | 2012-01-01  | 2013-01-01 |
| 2  | 2013-01-02  | 2013-01-03 |
| 3  | 2014-01-01  | 2014-12-31 |
| 3  | 2015-06-01  | 2015-12-31 |

**Node 2**:

| id | date\_start | date\_end  |
|:---|:------------|:-----------|
| 1  | 2011-07-01  | 2012-07-02 |
| 2  | 2013-01-03  | 2013-01-04 |
| 3  | 2017-01-01  | 2017-12-31 |

The following join query is our starting point for the date aggregation. It contains the cross-product of the validity
date ranges of both tables.

```sql
select coalesce("node_1"."id", "node_2"."id") as "primary_column",
       "node_1"."date_start"                  as "date_start_1",
       "node_1"."date_end"                    as "date_end_1",
       "node_2"."date_start"                  as "date_start_2",
       "node_2"."date_end"                    as "date_end_2"
from "node_1"
         join "node_2"
              on "node_1"."id" = "node_2"."id";
```

| primary\_column | date\_start\_1 | date\_end\_1 | date\_start\_2 | date\_end\_2 |
|:----------------|:---------------|:-------------|:---------------|:-------------|
| 1               | 2012-01-01     | 2013-01-01   | 2011-07-01     | 2012-07-02   |
| 2               | 2013-01-02     | 2013-01-03   | 2013-01-03     | 2013-01-04   |

## `MERGE`

To `MERGE` the dates, we'll have to create 5 CTEs:

- `overlap`: entries where the date ranges of both nodes overlap.
- `no_overlap`: entries where the date ranges of both nodes do not overlap.
- `left_node_no_overlap`: entries with all unmatched date ranges from the left join node.
- `right_node_no_overlap`: entries with all unmatched date ranges from the right join node.
- `merge`: the union of all entries from the `overlap`, `left_node_no_overlap` and `right_node_no_overlap`.

The result of the `merge` CTE will contain the newly created ranges.

### `overlap`

The `overlap` CTE will the show overlapping ranges by selecting the least of all start dates and the greatest of all
end dates where all starts are non-null and there is an overlap between the date ranges. This is ensured by comparing
the maximum of the start dates with the minimum of the end dates.

```sql
select "primary_column",
       least("date_start_1", "date_start_2") as "range_start",
       greatest("date_end_1", "date_end_2")  as "range_end"
from "joined_node"
where (
              "date_start_1" is not null
              and "date_start_2" is not null
              and greatest("date_start_1", "date_start_2") < least("date_end_1", "date_end_2")
          )
```

Looking at our base table, we can conclude that the date range `2012-01-01` to `2013-01-01` from the left node and the
date range `2011-07-01` to `2012-07-02` from the right node clearly overlap. The result of the overlap table computes
the new maximum overlapping range from `2011-07-01` to `2013-01-01`.

| primary\_column | range\_start | range\_end |
|:----------------|:-------------|:-----------|
| 1               | 2011-07-01   | 2013-01-01 |

### `no_overlap` - intermediate table

This CTE includes every row that isn’t part of the previous `overlap` CTE by inverting its conditions. It’s then used
as an intermediate table for the following two steps.

```sql
select "primary_column",
       "date_start_1",
       "date_end_1",
       "date_start_2",
       "date_end_2"
from "joined_node"
where (
              "date_start_1" is null
              or "date_start_2" is null
              or not greatest("date_start_1", "date_start_2") < least("date_end_1", "date_end_2")
          )
```

| primary\_column | date\_start\_1 | date\_end\_1 | date\_start\_2 | date\_end\_2 |
|:----------------|:---------------|:-------------|:---------------|:-------------|
| 2               | 2013-01-02     | 2013-01-03   | 2013-01-03     | 2013-01-04   |
| 3               | 2014-01-01     | 2014-12-31   | 2017-01-01     | 2017-12-31   |
| 3               | 2015-06-01     | 2015-12-31   | 2017-01-01     | 2017-12-31   |

### `left_node_no_overlap`

This query selects all unmatched date ranges from the left join partner.

```sql
select "primary_column",
       "date_start_1" "range_start",
       "date_end_1"   "range_end"
from "no_overlap"
where "date_start_1" is not null
```

| primary\_column | range\_start | range\_end |
|:----------------|:-------------|:-----------|
| 2               | 2013-01-02   | 2013-01-03 |
| 3               | 2014-01-01   | 2014-12-31 |
| 3               | 2015-06-01   | 2015-12-31 |

### `right_node_no_overlap`

```sql
select "primary_column",
       "date_start_2" "range_start",
       "date_end_2"   "range_end"
from "no_overlap"
where "date_start_2" is not null
```

| primary\_column | range\_start | range\_end |
|:----------------|:-------------|:-----------|
| 2               | 2013-01-03   | 2013-01-04 |
| 3               | 2017-01-01   | 2017-12-31 |
| 3               | 2017-01-01   | 2017-12-31 |

### `merge`

In the final step, we'll merge the `overlap`, `left_node_no_overlap` and `right_node_no_overlap` together.
The result of the CTE will then contain all overlapping ranges as well as all non-overlapping ranges from the left and
right join partners.

```sql
select *
from "overlap"
union all
select *
from "left_node_no_overlap"
union all
select *
from "right_node_no_overlap"
```

We can use a `union all` instead of a `union distinct`. It's faster, and because we have to apply interval packing
again anyway, which will ensure a unique set of date ranges in the final result of the `MERGE` aggregation.

| primary\_column | range\_start | range\_end |
|:----------------|:-------------|:-----------|
| 1               | 2011-07-01   | 2013-01-01 |
| 2               | 2013-01-02   | 2013-01-03 |
| 3               | 2014-01-01   | 2014-12-31 |
| 3               | 2015-06-01   | 2015-12-31 |
| 2               | 2013-01-03   | 2013-01-04 |
| 3               | 2017-01-01   | 2017-12-31 |
| 3               | 2017-01-01   | 2017-12-31 |

The result contains the newly created ranges - with some overlapping ranges for the subject with id 3. We therefore
again
apply the steps from interval packing which gives us the final result of the `MERGE` aggregation.

| primary\_column | range\_start\_min | range\_end\_max |
|:----------------|:------------------|:----------------|
| 3               | 2014-01-01        | 2014-12-31      |
| 2               | 2013-01-02        | 2013-01-04      |
| 3               | 2015-06-01        | 2015-12-31      |
| 3               | 2017-01-01        | 2017-12-31      |
| 1               | 2011-07-01        | 2013-01-01      |

## `INTERSECT`

Suppose again that we have two concepts, each containing a date range set with only unique date range values.
The starting point of the date aggregation process remains the joined node of the two nodes.

To `INTERSECT` the dates, we'll have to create 3 CTEs:

- `overlap`: entries where the date ranges of both nodes overlap.
- `no_overlap`: entries where the date ranges of both nodes do not overlap. In contrast to merging, non-overlapping
  and missing fields are handled as nulls.
- `merge`: the union of all entries from the `overlap` and `no_overlap`

### `overlap`

The `overlap` CTE will the show intersecting ranges: In contrast to the `MERGE` aggregation, we use the `GREATEST` start
and `LEAST` end to get only the intersection of both ranges. Note that the `where` condition of the overlap is the same:
so we filter for entries that overlap, but ensure that only the intersection of the ranges is selected.

```sql
select "primary_column",
       greatest("date_start_1", "date_start_2") as "range_start",
       least("date_end_1", "date_end_2")        as "range_end"
from "joined_node"
where (
              "date_start_1" is not null
              and "date_start_2" is not null
              and greatest("date_start_1", "date_start_2") < least("date_end_1", "date_end_2")
          )
```

Looking at our base table, we can conclude that the date range `2012-01-01` to `2013-01-01` from the left node and the
date range `2011-07-01` to `2012-07-02` from the right node clearly overlap. The result of the overlap table computes
the new maximum intersection of both ranges: `2012-01-01` to `2012-07-02`.

| primary\_column | range\_start | range\_end |
|:----------------|:-------------|:-----------|
| 1               | 2012-01-01   | 2012-07-02 |

### `no_overlap` - intermediate table

This CTE includes every row that isn’t part of the previous `overlap` CTE by inverting its conditions. Because we now
the entries of this table have no intersecting validity date values, we null their validity date entries.

```sql
select "primary_column",
       null as "range_start",
       null as "range_end"
from "joined_node"
where (
              "date_start_1" is null
              or "date_start_2" is null
              or not greatest("date_start_1", "date_start_2") < least("date_end_1", "date_end_2")
          )
```

We need this table because we don't want to filter out subjects that have no overlapping range, we just assign them an
empty validity date range.

| primary\_column | range\_start | range\_end |
|:----------------|:-------------|:-----------|
| 2               | null         | null       |

### `merge`

In the final step, we'll merge the `overlap` and `no_overlap` table together.
The result of the CTE will then contain all overlapping ranges as well as all non-overlapping ranges.

```sql
select *
from "overlap"
union all
select *
from "no_overlap"
```

| primary\_column | range\_start | range\_end |
|:----------------|:-------------|:-----------|
| 1               | 2012-01-01   | 2012-07-02 |
| 2               | null         | null       |

## `NEGATE`

The idea on how to negate or respectively invert a set of date ranges was taken from this
[blog post](https://explainextended.com/2009/11/09/inverting-date-ranges).

Take the following table containing a date set of unique date ranges as starting point,
sorted by `range_start`. Like always, the `range_end` date is considered excluded.

| primary\_column | range\_start | range\_end |
|:----------------|:-------------|:-----------|
| 1               | 2012-01-01   | 2013-01-01 |
| 1               | 2015-01-01   | 2016-01-01 |
| 1               | 2018-01-01   | 2019-01-01 |

To invert these ranges, we will need 2 CTEs:

- `row_numbers`: assigns row numbers to validity date rows.
- `inverted_dates`: makes a self-join but using the row number shifted up by one as join condition. This way, we get
  the intervals between the start and ends of the respective validity date intervals.

### `row_numbers`

We assign row numbers to the ordered validity dates while partitioning by primary column.

```sql
select "primary_column",
       "range_start",
       "range_end",
       row_number() over (
           partition by "primary_column"
           order by "range_start"
           ) as "row_number"
from "table"
```

This gives us the following result:

| primary\_column | range\_start | range\_end | row\_number |
|:----------------|:-------------|:-----------|:------------|
| 1               | 2012-01-01   | 2013-01-01 | 1           |
| 1               | 2015-01-01   | 2016-01-01 | 2           |

### `inverted_dates`

Now, we do a self-join, selecting the end dates of the left join parter and the start dates of the right join partner,
but shifted up by 1 row as join condition. This would result in the first start and last end date of each partition
being `null`. In this case, we replace the`null` start date value with the minimum possible date and the `null` end
date value with the maximum possible date value.

```sql
select coalesce("rows_left"."pid", "rows_right"."pid") as "primary_column",
       coalesce(
               "rows_left"."range_end",
               TO_DATE('0001-01-01', 'yyyy-mm-dd')
       ) as "range_start",
       coalesce(
               "rows_right"."range_start",
               TO_DATE('9999-12-31', 'yyyy-mm-dd')
       ) as "range_end"
from "row_numbers" "rows_left"
         full outer join "row_numbers" "rows_right"
                         on (
                                     "rows_left"."pid" = "rows_right"."pid"
                                 and ("rows_left"."row_number" + 1) = "rows_right"."row_number"
                             )
```

This gives us the following result:

| primary\_column | range\_start | range\_end |
|:----------------|:-------------|:-----------|
| 1               | 0001-01-01   | 2012-01-01 |
| 1               | 2013-01-01   | 2015-01-01 |
| 1               | 2016-01-01   | 9999-12-31 |

First, we got the interval from the first possible date to the start of the first validity date: `0001-01-01` to
`2012-01-01`. Second, we have the interval between the two original intervals from `2013-01-01` to `2015-01-01`. Last,
we got the interval from the last end date to the maximum possible date: `2016-01-01` to `9999-12-31`.

# Appendix

### Whole `MERGE` query

```sql
with "joined_node" as (select coalesce("node_1"."id", "node_2"."id") as "primary_column",
                              "node_1"."date_start"                  as "date_start_1",
                              "node_1"."date_end"                    as "date_end_1",
                              "node_2"."date_start"                  as "date_start_2",
                              "node_2"."date_end"                    as "date_end_2"
                       from "node_1"
                                join "node_2"
                                     on "node_1"."id" = "node_2"."id"),
     "overlap" as (select "primary_column",
                          least("date_start_1", "date_start_2") as "range_start",
                          greatest("date_end_1", "date_end_2")  as "range_end"
                   from "joined_node"
                   where (
                                 "date_start_1" is not null
                                 and "date_start_2" is not null
                                 and greatest("date_start_1", "date_start_2") < least("date_end_1", "date_end_2")
                             )),
     "no_overlap" as (select "primary_column",
                             "date_start_1",
                             "date_end_1",
                             "date_start_2",
                             "date_end_2"
                      from "joined_node"
                      where (
                                    "date_start_1" is null
                                    or "date_start_2" is null
                                    or not greatest("date_start_1", "date_start_2") < least("date_end_1", "date_end_2")
                                )),
     "left_node_no_overlap" as (select "primary_column",
                                       "date_start_1" "range_start",
                                       "date_end_1"   "range_end"
                                from "no_overlap"
                                where "date_start_1" is not null),
     "right_node_no_overlap" as (select "primary_column",
                                        "date_start_2" "range_start",
                                        "date_end_2"   "range_end"
                                 from "no_overlap"
                                 where "date_start_2" is not null),
     "merge" as (select *
                 from "overlap"
                 union all
                 select *
                 from "left_node_no_overlap"
                 union all
                 select *
                 from "right_node_no_overlap"),
     "previous_end" as
         (select "primary_column",
                 "range_start",
                 "range_end",
                 max("range_end") over (
                     partition by "primary_column"
                     order by "range_start", "range_end"
                     rows between unbounded preceding and 1 preceding
                     ) as "previous_end"
          from "merge"),
     "range_index" as
         (select "primary_column",
                 "range_start",
                 "range_end",
                 sum(case
                         when "range_start" > "previous_end" then 1
                         else null
                     end) over (
                     partition by "primary_column"
                     order by "range_start", "range_end"
                     rows unbounded preceding
                     ) "range_index"
          from "previous_end")
select "primary_column",
       min("range_start") "range_start_min",
       max("range_end")   "range_end_max"
from "range_index"
group by "primary_column", "range_index"
```

### Whole `INTERSECT` query

```sql
with "joined_node" as (select coalesce("node_1"."id", "node_2"."id") as "primary_column",
                              "node_1"."date_start"                  as "date_start_1",
                              "node_1"."date_end"                    as "date_end_1",
                              "node_2"."date_start"                  as "date_start_2",
                              "node_2"."date_end"                    as "date_end_2"
                       from "node_1"
                                join "node_2"
                                     on "node_1"."id" = "node_2"."id"),
     "overlap" as (select "primary_column",
                          greatest("date_start_1", "date_start_2") as "range_start",
                          least("date_end_1", "date_end_2")        as "range_end"
                   from "joined_node"
                   where (
                                 "date_start_1" is not null
                                 and "date_start_2" is not null
                                 and greatest("date_start_1", "date_start_2") < least("date_end_1", "date_end_2")
                             )),
     "no_overlap" as (select "primary_column",
                             null::date as "range_start",
                             null::date as "range_end"
                      from "joined_node"
                      where (
                                    "date_start_1" is null
                                    or "date_start_2" is null
                                    or not greatest("date_start_1", "date_start_2") < least("date_end_1", "date_end_2")
                                ))
select *
from "overlap"
union all
select *
from "no_overlap";
```

### Whole `NEGATE` query

```sql
with "row_numbers" as (select "primary_column",
                              "range_start",
                              "range_end",
                              row_number() over (
                                  partition by "primary_column"
                                  order by "range_start"
                                  ) as "row_number"
                       from "table")
select coalesce("rows_left"."pid", "rows_right"."pid") as "primary_column",
       coalesce(
               "rows_left"."range_end",
               TO_DATE('0001-01-01', 'yyyy-mm-dd')
       )                                               as "range_start",
       coalesce(
               "rows_right"."range_start",
               TO_DATE('9999-12-31', 'yyyy-mm-dd')
       )                                               as "range_end"
from "row_numbers" "rows_left"
         full outer join "row_numbers" "rows_right"
                         on (
                                     "rows_left"."pid" = "rows_right"."pid"
                                 and ("rows_left"."row_number" + 1) = "rows_right"."row_number"
                             )
```
