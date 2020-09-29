# conquery-frontend

## Technologies

- ES6
- react / redux
- webpack
- simple express.js server for a mock api

## Run (production)

### Using docker

**Requirements**

- `docker`

**Linux / OS X**

```
./scripts/start_production.sh
```

**Windows**

Commands analogoues to `start_production.sh` script.

### Using node / express


```
yarn
yarn run build
PORT=8000 yarn run start-production
```

## Available Environment Variables

````js
NODE_ENV: "development" | "production"; // Create a production build and disable debuggers when set to production   
REACT_APP_API_URL?: string; // Set the backend URL
REACT_APP_DISABLE_LOGIN?: boolean; // Disable forced login page
REACT_APP_LANG?: "de" | "en"; // Select language for Frontend
PORT?: string; // Port on which to start the frontend
````


## Install & start (development)

**Requirements**

- `node` 10 (LTS)
- `yarn`

**Install**

```bash
$ yarn
```

**Start**

```bash
$ yarn start
```

**Mock API**

```bash
$ yarn serve
```

**Login**

When queried for login:

- **Username**: `test`
- **Password**: `test`

This is documented in [the mock-API](https://github.com/bakdata/conquery/blob/develop/frontend/mock-api/index.js).

## Glossary

Depending on the use-case, we're still calling the same concepts differently sometimes. Here is an explanation.

- **Concept Tree** – consists of concepts
- **Concept Tree Node / Concept** – a concept node
- **Query**
  - consisting of multiple `and`-groups
  - which again consist of multiple `or`-ed concepts
  - which again are applied on different tables
  - which again may contain certain filters for table columns
- **Query Editor** – on the right, used to construct a query interactively
- **Query And Group / Group** – column in the query editor
- **Query Node / Element** – one node in the query editor, either a concept or a previous query
- **Previous Query / Stored Query** – a previous query that has been saved in the backend database for future use (as itself or within other queries)
- **Dataset / Database** – data set that is used to ask queries against
- **Tooltip** – small area (below), that contains additional information on hovering over certain elements
- **Additional Infos** – data (key-value pairs) that are part of concept nodes and can be displayed inside the tooltip

## Various Technical Explanations

Migration to TypeScript is in progress.

### Drag and Drop

- We're using MultiBackend to support Drag and Drop for touch and html5.
- To render a Drag and Drop preview on mobile, we'll have to calculate `width` and `height` of the drag source.

