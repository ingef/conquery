# SQL connector extraction verification

## Reference revision

Fetched `origin/develop` on 2026-09-21: `353097249dd1ec4b79a02931996191c9fe39660d`.
The local `develop` branch remains at `93b4134e01ad804997303ad253fc0c9d4d64ac75`; it has not been moved.
Use the fetched revision for the extraction-only portability checkout.

The SQL sum, count, and duration aggregator implementations have no differences between these two revisions. Compatibility assertions must be checked against this recorded `develop` source, not merely against the migration branch.

## Aggregation coverage checked so far

| Operation | SQL behavior on the reference revision | Existing fixture or evidence | Extraction obligation |
| --- | --- | --- | --- |
| Ordinary sum with `subtractColumn` | Subtracts, treating a single missing operand as zero and preserving NULL when both are missing | `aggregator/SUM_DIFF_AGGREGATOR`, `filter/DIFFSUM_INTEGER`, `filter/DIFFSUM_REAL`; `SumSqlAggregator.createSumAggregationSelect` | Preserve subtraction and input numeric types |
| Sum with `distinctByColumn` | Partitions by entity IDs and distinct keys, selects row number 1, and sums the values | `aggregator/SUM_DISTINCT_AGGREGATOR`; `SumSqlAggregator.createDistinctSumAggregationSelect` | Preserve the separate predecessor CTEs and grouping |
| Sum with both options | The SQL distinct branch does not pass `subtractColumn` into its helper; it sums the original column | Both `connectorSelect` and `convertToSqlFilter` in `SumSqlAggregator` | Preserve this combined-option SQL quirk; do not remove either independently supported option |
| In-memory sum with both options | Wraps the subtracting aggregator in `DistinctValuesWrapperAggregator` | `SumSelect.createAggregator/getAggregator` and `SumFilter.createFilterNode/getAggregator` | Not the SQL compiler baseline; leave these implementations unchanged |
| Duration sum with `distinctBy` | SQL date-range aggregation does not consume distinct keys | `aggregator/DURATION_SUM_DISTINCT_AGGREGATOR/DURATION_SUM.test.json` explicitly disables SQL testing with comment `distinctBy not yet implemented.` | Preserve the existing SQL path; do not introduce a rejection in compilation |
| Count with configured distinct keys | SQL checks `isDistinct` and counts distinct values of the counted column; it does not read `distinctByColumn` | `CountSqlAggregator.createCountAggregationSelect`; `filter/COUNT_DISTINCT_MULTI` exists but has not been executed here | Preserve SQL behavior separately from in-memory distinct-key behavior |

Fixture paths in the table are relative to `backend/src/test/resources/tests/` on the reference revision. Source inspection establishes intended compatibility targets; it is not evidence that database integration tests have passed.

## Local evidence

- Initial connector baseline: 157 tests passed.
- Initial backend package with tests skipped: passed, including test compilation.
- Sum regressions reproduced before correction: subtraction incorrectly included in distinct sums; DECIMAL expressions incorrectly typed as BigDecimal.
- After correction: 158 connector tests passed.
- Duration distinct-key rejection reproduced and removed to match the existing SQL path.
- Backend public-entry-point characterization: five aggregation tests and one dialect test passed after delegating sum, count, quarter count, and duration sum to the connector. The selected connector aggregation tests also passed (13 tests).
- The ClickHouse nullable date-bound regression failed before the dialect bridge and passed afterward; preserving `Nullable(Date32)` prevents missing outer-join dates from becoming epoch dates.
- Final connector run for the numeric aggregation extraction: 158 tests passed (`./mvnw.cmd -o -B -ntp -pl sql-connector -am test`). HANA/ClickHouse connections were not opened.

## Pending verification

Extraction is still in progress. The remaining operation/dialect coverage, full extraction-only `develop` build, and HANA/ClickHouse result-level verification are pending. Database integration suites cannot run on this host; a final handoff will identify the tested extraction revision, commands, prerequisites, and required reports.
