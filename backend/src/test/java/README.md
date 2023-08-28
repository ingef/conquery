# Specify test path for query.json test specs

Pass environment variable `CONQUERY_TEST_DIRECTORY=tests/foo/bar/fooBar/`
to only run tests from `src/test/resources/tests/foo/bar/fooBar/`.
Default test root dir is `src/test/resources/tests/`.

**Don't forget the trailing `/`!**

# Specify test path for SQL backend tests

Override `SQL_TEST_DIRECTORY` (default set to `src/test/resources/tests/sql/`).

# Useful HANA commands

To use a remote HANA instance set `USE_LOCAL_HANA_DB=false` before running the tests.

```sql
-- get all tenant databases
select *
from "SYS"."M_DATABASES";
-- get all users of the current database 
select *
from "SYS"."USERS";

CREATE
USER BAKDATA PASSWORD "FooBar1";
ALTER
USER BAKDATA DISABLE PASSWORD LIFETIME; -- otherwise password has to be changed from time to time 
ALTER
USER BAKDATA PASSWORD "FizzBuzz2";
```
