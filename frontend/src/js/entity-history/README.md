# entity-history

This documents how the frontend contructs a timeline UI from an entity CSV containing events.

First, we load the CSV for a single entity (+ for a certain set of sources). We expect that CSV to contain on average ~300 events for an entity and up to ~3000 events in few cases. The total number depends on the individual entity and on the number of sources that are queried for. A source corresponds to a DB table that contains the events.

For every column of the CSV, we receive a **column description**, that specifies further meta data about the column, mainly:

- its data type (date, string, number, money, concept ...)
- some semantic information, like the root concept of a column containing concept ids

It's implicit that every row (= every event) has

- a `source`, the DB table that the events belong to
- `dates`, which is a set of day ranges

We assume the `dates` to either be a single day (start and end day are the same), or to be exactly one day range (from day, to another day). In case of multiple day ranges, we're simply taking

- the first day of the first range
- the last day of the last range

We're currently omitting the details in between.

We're assuming that the events from the CSV are generally sorted by date.

We go through the events and group them by **year**. Within every year, we group the events by **quarter**.

With this information, we can already create a timeline that highlights different years and quarters.

But we found that oftentimes, many consecutive events from our data sets contained redundand information.

So within a quarter, we further group the events by **dates and source and secondary ids**. That means when multiple consecutive events from the same source happen at the same dates and share the exact same secondary ids, we group them. Within those groups, we calculate **all differences** between **the values of all columns** of the event.

Columns that are not part of the found differences have the same value accross all events. Then we can display the event groups in the timeline as a single item, highlighting the commonalities as well as the differences.

For all column values, we can display them based on the type (derived from the column description).
- for concept columns, we're resolving concept labels from our concept trees
- for money columns, we're formatting the value as a currency