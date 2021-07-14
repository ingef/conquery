# conquery-frontend

## Technologies

- create-react-app
- typescript
- react / redux
- simple express.js server for a mock api
- keycloak server for authentication, local setup using docker-compose

# Some notable libraries
- redux-form
- react-dnd
- i18next

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
docker run -d -e REACT_APP_API_URL=https://some-other-conquery-api.com -p 8000:8000 -name frontend frontend
```

**Windows**

Commands analogoues to `start_production.sh` script.

## Development

**Requirements**

- `node` >= 14
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

## Various Technical Explanations

- Migration from Flow to TypeScript is in progress. At the moment, Typescript errors are printed to console on server start and build, to see what they are and to fix them. Also to check how many are left using `yarn typecheck`. But type errors are ignored (see `.env`) to be able to still compile for now. Plan is to fix the errors step by step and then to enable errors on start / build again.
- Emotion is used for theming and styles. Some sass styles are left. Plan is to migrate (back) to styled-components, because that's more TypeScript compatible in some edge cases like generic component props (see usage of Dropzone). Plan is also to remove the remaining sass styles.
- Redux actions aren't typed well yet. Plan is to migrate fully to https://github.com/piotrwitek/typesafe-actions
- We're moving away from redux middlewares like thunk (replaced by hooks) and multi 
- Redux Form is still heavily in use. In consequence, many of the input components use a less than optimal `input`: { value, onChange }` props structure. Plan is to replace Redux Form with something more modern that's using either context or local state.
- We're still trying to support IE11. But this is phasing out slowly, since most users are already using more modern Browsers.

### Drag and Drop

- We're using react-dnd (and we like it).
- We're using MultiBackend to support Drag and Drop for touch and html5.
- To render a Drag and Drop preview on mobile, we'll have to calculate `width` and `height` of the drag source.
