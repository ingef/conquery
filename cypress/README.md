# Organization of the integration tests

## Test file structure

For now (until we come up with something else), we're naming our integration test files using numbers, in order

- to specify an execution order
- to help our mental models

going from basic "smoke test" like visiting the page, to more advanced features.

**But:** the tests in every file should technically run independent from each other and in any order.

## Test-IDs

We're using `data-test-id` attributes in the frontend to help us selecting the right DOM elements in the tests.

**Conventions**

- we're writing the ids in dashed-case, e.g. `left-pane-container`
- we're trying to make the ids unique. So e.g. in lists, we prefer using a single `xyz-list` id for the container instead of `xyz-list-item` for every item.

## Helpers

Some basic steps can be reused in multiple tests, like visiting a page and waiting for it to load.

- we're placing these steps in `./integration-helpers`
- we're trying to avoid extracting too fine-grained steps, in order to keep the tests as plain, simple, and easy to read as possible.