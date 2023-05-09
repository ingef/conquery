select pid, (extract(century from date '2012-12-31') - extract(century from datum)) as "date_distance" from table1 where geschlecht in ('f')
