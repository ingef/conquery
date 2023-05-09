select pid, (extract(year from date '2012-12-31') - extract(year from datum)) as "date_distance" from table1 where geschlecht in ('f')
