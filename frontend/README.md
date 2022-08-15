# conquery-frontend

## Technologies

- create-react-app
- typescript
- react / redux
- simple express.js server for a mock api
- keycloak server for authentication, local setup using docker-compose

# Some notable libraries in use
- react-dnd
- i18next
- react-hook-form

## Setup

For all further steps, set up environment variables. Copy the `.env.example` file:

```bash
cp .env.example .env
```

and adjust the `.env` file if necessary. They're used at build time.

## Run (production)

### Using docker

**Requirements**

- `docker`

**Linux / OS X**

```bash
./scripts/start_production.sh
```

You can also adjust the docker run command, to pass env variables at runtime.

Example:

```bash
docker run -d -e REACT_APP_API_URL=https://some-other-conquery-api.com -p 8000:80 -name frontend frontend
```

**Windows**

Commands analogoues to `start_production.sh` script.

## Development

**Requirements**

- `node` >= 16
- `yarn`

**Install and start**

```bash
yarn
yarn start
```

Adjust your local `.env` file as necessary to apply environment variables during development

**Mock API**

```bash
$ yarn server
```

**Login**

When queried for login:

- **Username**: `test`
- **Password**: `test`

This is documented in [the mock-API](https://github.com/bakdata/conquery/blob/develop/frontend/mock-api/index.js).

**Code formatting**  [![code style: prettier](https://img.shields.io/badge/code_style-prettier-ff69b4.svg?style=flat-square)](https://github.com/prettier/prettier)

The frontend TypeScript code is formatted using `prettier`.

We recommend you configure your editor to auto-format on save. If you're using VS-Code, for example, there's a plugin: [Prettier – Code formatter](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode).

You could also invoke prettier on the command line:
```
yarn prettier --write /path/to/file
```

## Glossary

Depending on the use-case, we're still calling the same concepts differently sometimes. Here is an explanation.

- **Concept Tree** – consists of concepts
- **Concept Tree Node / Concept** – queries consist mainly of concepts
- **Query**
  - consisting of multiple `and`-groups
  - which again consist of multiple `or`-ed concepts
  - which again are applied on different tables
  - which again may contain certain filters for table columns
- **Query Editor** – on the right, used to construct a query interactively, using drag and drop
- **Query And Group / Group** – column in the query editor
- **Query Node / Element** – one node in the query editor, either a concept or a previous query
- **Previous Query / Stored Query** – a previous query that has been saved in the backend database for future use (as itself or within other queries)
- **Dataset / Database** – data set that is used to ask queries against
- **Tooltip** – small area (below), that contains additional information on hovering over certain elements
- **Additional Infos** – data (key-value pairs) that are part of concept nodes and can be displayed inside the tooltip

## Technical Explanations (mini ADRs – "architectural decision records")

### Issues
- We're getting a lot of "Failed to parse source maps"-warnings when starting the dev server because we're using react-keycloak. There is [an open issue for that](https://github.com/react-keycloak/react-keycloak/issues/176), and we hope this gets resolved soon.

### Types

- Before migrating to TypeScript, the code used a few Flow types here and there and relied on object mutation and other patterns that feel a little like quirky today / would be written differently with TypeScript. Some remainders of that time might still be present in the code.

### Hooks
- We're using react hooks extensively, but we're **not** using useCallback in a lot of places yet. So in general, we've been avoiding passing callbacks into the dependency arrays of useEffect / useMemo / etc. Probably, we should introduce more useCallback gradually, while making sure we don't introduce infinite loops.

### Styles

- Emotion is used for theming and styles. The plan is to migrate (back) to styled-components or to another css-in-js solution, because emotion's "styled" is less TypeScript compatible in some edge cases like generic component props (see usage of Dropzone).

### State

- We're using [typesafe-actions](https://github.com/piotrwitek/typesafe-actions) for redux actions.
- We're moving away from redux-thunk (replaced mostly by hooks)
- We've migrated from `redux-form` to `react-hook-form` recently.

### Browser support

- We have been supporting IE11 in the past. Now we're supporting Chrome, Firefox. Safari should be compatible as well, but we rarely check that. Most remainders of IE-Support (polyfills / shims / workarounds) should be gone from the code base. We'll need to remove any remaining, if there are any.

### Drag and Drop

- We're using react-dnd (and we like it).
- We're using MultiBackend to support Drag and Drop for touch and html5.
- To render a Drag and Drop preview on mobile, we'll have to calculate `width` and `height` of the drag source.
