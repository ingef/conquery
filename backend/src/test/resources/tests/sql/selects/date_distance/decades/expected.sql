select pid, (extract(decade from date '2012-12-31') - extract(decade from datum)) as "date_distance" from table1 where geschlecht in ('f')
