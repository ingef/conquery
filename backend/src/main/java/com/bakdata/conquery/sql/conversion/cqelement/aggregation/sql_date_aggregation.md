# SQL date aggregation

When joined nodes provide multiple validity-date ranges, `AnsiSqlDateAggregator` combines them in one of two ways:

- `MERGE` produces the union of the inputs and then applies interval packing.
- `INTERSECT` produces their common intersection.

`NEGATE` is a separate operation that inverts an already aggregated set of intervals. `BLOCK` prevents validity dates
from being aggregated further up the query tree.

The examples below use two input ranges, but the generated projections and `UNION ALL` branches are built from every
validity-date input and therefore also apply to joins of three or more dated nodes. Range ends are exclusive.

The joined step supplies IDs, carry-through selects, and each input range:

```sql
with "joined_node" as (
    select coalesce("node_1"."id", "node_2"."id") as "primary_column",
           "node_1"."date_start"                  as "date_start_1",
           "node_1"."date_end"                    as "date_end_1",
           "node_2"."date_start"                  as "date_start_2",
           "node_2"."date_end"                    as "date_end_2"
    from "node_1"
    full outer join "node_2" on "node_1"."id" = "node_2"."id"
)
```

## `MERGE`

Before interval packing, `MERGE` adds exactly two date-aggregation CTEs, independent of the number of input ranges:

```text
joined -> no_overlap -> merge -> interval packing
```

### `no_overlap`

`no_overlap` is a shared intermediate CTE. It contains the joined rows that are not handled by the overlapping-range
branch. Keeping it shared avoids repeating the complete non-overlap predicate in every input-range branch.

```sql
"no_overlap" as (
    select "primary_column",
           "date_start_1",
           "date_end_1",
           "date_start_2",
           "date_end_2"
    from "joined_node"
    where "date_start_1" is null
       or "date_start_2" is null
       or not greatest("date_start_1", "date_start_2")
                  < least("date_end_1", "date_end_2")
)
```

The condition deliberately preserves SQL three-valued logic. For example, a row whose starts are non-null but whose
end comparison is `UNKNOWN` is not selected merely because it did not satisfy the overlap comparison.

### `merge`

The base SELECT of `merge` reads directly from `joined_node`. When all starts are present and the inputs overlap, it
emits the outer bounds of the overlapping ranges. Each inline `UNION ALL` branch reads from the shared `no_overlap`
CTE and emits one input range. IDs and carry-through selects are present in every branch.

```sql
"merge" as (
    select "primary_column",
           least("date_start_1", "date_start_2") as "range_start",
           greatest("date_end_1", "date_end_2")  as "range_end"
    from "joined_node"
    where "date_start_1" is not null
      and "date_start_2" is not null
      and greatest("date_start_1", "date_start_2")
              < least("date_end_1", "date_end_2")

    union all

    select "primary_column",
           "date_start_1" as "range_start",
           "date_end_1"   as "range_end"
    from "no_overlap"
    where "date_start_1" is not null

    union all

    select "primary_column",
           "date_start_2" as "range_start",
           "date_end_2"   as "range_end"
    from "no_overlap"
    where "date_start_2" is not null
)
```

The merge result stays a relational boundary because interval-packing window functions must see the complete union.
`UNION ALL` is intentional: interval packing combines overlapping or duplicate ranges afterwards.

The `QueryStep` predecessor chain remains linear (`joined -> no_overlap -> merge`). Although the merge base SELECT
references `joined_node`, it does not list that step as a second predecessor: `joined_node` has already been emitted
transitively through `no_overlap`, and the renderer does not deduplicate predecessor DAGs.

## `INTERSECT`

`INTERSECT` adds no date-aggregation CTE. It folds the result projection and condition into the joined step:

```text
joined (with intersection projection)
```

For an overlapping row, CASE expressions emit the greatest start and least end. For a non-overlapping row or a row
with a missing input start, they emit typed SQL `NULL` values. The WHERE clause retains the effective condition of the
former `overlap UNION ALL no_overlap` pipeline.

```sql
select "primary_column",
       case
           when "date_start_1" is not null
            and "date_start_2" is not null
            and greatest("date_start_1", "date_start_2")
                    < least("date_end_1", "date_end_2")
           then greatest("date_start_1", "date_start_2")
           else cast(null as date)
       end as "range_start",
       case
           when "date_start_1" is not null
            and "date_start_2" is not null
            and greatest("date_start_1", "date_start_2")
                    < least("date_end_1", "date_end_2")
           then least("date_end_1", "date_end_2")
           else cast(null as date)
       end as "range_end"
from "node_1"
full outer join "node_2" on "node_1"."id" = "node_2"."id"
where (
          "date_start_1" is not null
      and "date_start_2" is not null
      and greatest("date_start_1", "date_start_2")
              < least("date_end_1", "date_end_2")
      )
   or (
          "date_start_1" is null
       or "date_start_2" is null
       or not greatest("date_start_1", "date_start_2")
                  < least("date_end_1", "date_end_2")
      )
```

The effective `overlapCondition OR noOverlapCondition` filter must not be replaced with an unconditional projection.
If nullable expressions make the date comparison `UNKNOWN` while all starts are non-null, both sides can remain
non-true and the row must still be excluded, matching the previous union pipeline.

## `NEGATE`

Negation continues to use two CTEs:

```text
final aggregated intervals -> row_numbers -> inverted_dates
```

`row_numbers` assigns an ordered row number after grouping, group filters, interval packing, multi-connector
aggregation, and compound logical-node aggregation have completed:

```sql
"row_numbers" as (
    select "primary_column",
           "range_start",
           "range_end",
           row_number() over (
               partition by "primary_column"
               order by "range_start"
           ) as "row_number"
    from "final_aggregated_intervals"
)
```

`inverted_dates` self-joins that materialized result with row numbers shifted by one. This finds the gaps before,
between, and after the aggregated ranges without calculating the window twice:

```sql
select coalesce("rows_left"."primary_column", "rows_right"."primary_column") as "primary_column",
       coalesce("rows_left"."range_end", min_date())                           as "range_start",
       coalesce("rows_right"."range_start", max_date())                       as "range_end"
from "row_numbers" as "rows_left"
full outer join "row_numbers" as "rows_right"
  on "rows_left"."primary_column" = "rows_right"."primary_column"
 and "rows_left"."row_number" + 1 = "rows_right"."row_number"
```

Keeping `row_numbers` as its own boundary is required for union-producing inputs and ensures numbering is applied to
the final aggregated interval set rather than to preprocessing or connector-level rows.
