# Conquery
*fast & efficient analysis*

[![Last Release](https://img.shields.io/github/release-date/bakdata/conquery.svg?logo=github)](https://github.com/bakdata/conquery/releases/latest)
![Code Size](https://img.shields.io/github/languages/code-size/bakdata/conquery.svg)
[![License](https://img.shields.io/github/license/bakdata/conquery.svg)](https://github.com/bakdata/conquery/blob/develop/LICENSE)


![conquery Screenshot](images/screenshot-v4.png)

Conquery is a powerful web-based tool to compose and execute queries against large event-like data sets.

Event data sets typically associate events with a certain subject (i.e. a person or a physical object). One common use case for the data is to identify groups of similar subjects based on the assumption that they share similar events in a given time frame.

Conquery supplies a powerful interface to group event types in a hierarchical *concept tree* structure. Elements of this tree represent a group of similar subjects. Those subjects can be composed into a powerful query that runs against the data set.

## Requirements
- Maven 3 (optional for building)
- Java JDK 11
- Node.js 18 + Yarn
- curl (to import the test data)


## Starting the demo

### Frontend only

This repository includes the Conquery frontend along with a non-functional backend. It provides a set of example concept trees to demonstrate the capabilities of the UI: The example's use case is to search for groups of actors who appeared in movies of the same genre or received the same award.

Check the README in `/frontend` for details.

### Frontend + Backend

#### Steps
To test frontend and backend together you can start the setup that is used for end-to-end tests.

First build the backend using `conquery/scripts/build_backend_version.sh` or download a JAR from
the [release page](https://github.com/bakdata/conquery/releases) and place it in `conquery/executable/target/`.

Build the frontend by running:

```bash
cd frontend
cp .env.example .env
yarn
yarn build
```

You can then run `conquery/scripts/run_e2e_all.sh` to start frontend and backend, and also load the test data required by cypress end-to-end test or you can run `conquery/scripts/run_e2e_backend.sh` and `conquery/scripts/run_e2e_frontend.sh` separately without loading any data.

After that, you can visit http://localhost:8081/admin-ui and explore the Admin Panel.

The frontend is accessible at http://localhost:8000 as the default "superuser" implicitly. Since the backend uses a
development authentication, you can switch users by passing another users "UserId" as the access token in the query
string when accessing the frontend, e.g.: http://localhost:8000/?access_token=user.user2.

## Development

### Testing

Apart from separate frontend and backend tests, the project also contains end-to-end tests powered by cypress.

To run the end-to-end test locally:

1. Make sure you installed all [requirements](#requirements)
2. From the repo root folder run  `conquery/scripts/run_e2e_all.sh`
3. Wait until the output: `Node server listening on port: 8000` appears
4. To install cypress and it's dependencies, run `yarn` from an other terminal in the `conquery/` folder
5. Then run `yarn cypress open` to start cypress
6. Then chose a test suite and start it.

For further informations on this and other tests, please refer to the corresponding [CI configuration](https://github.com/bakdata/conquery/tree/develop/.github/workflows).

### Data Integration

To make you own data (in form of CSVs) availiable to conquery some steps are necessary:

1. Describe your data table structure with meta data, by generating Import- and Table-JSONs.
2. Describe your data content as Concept-JSON to provide query functionallity.
3. Preprocess your data from CSV to CQPP.
4. Upload everything.

To get a better impression of the single steps, take a look at the [Tutorials](./tutorial/mimic_iii_demo/README.md)

### Custom Forms

It is possible to extend the analytic capabilities by adding custom forms.
Custom forms provide an easy way for a user to perform standardized analyses.
For more informations, take a look at the [guide](./docs/custom_forms.md)

## Acknowledgements

This platform was created by [InGef – Institut für angewandte Gesundheitsforschung Berlin GmbH](http://www.ingef.de/) in
cooperation with [bakdata GmbH](http://www.bakdata.com) and [Kai Rollmann](https://kairollmann.de/).

[<img alt="InGef – Institut für angewandte Gesundheitsforschung Berlin GmbH" src="images/ingef_logo.svg" height=50 align="top">](http://www.ingef.de/)
&emsp;
[<img alt="bakdata GmbH" src="images/bakdata_logo.svg" height=37 align="top">](http://www.bakdata.com)
&emsp;
[<img alt="Rollmann Software" src="images/rollmann_software_logo.png" height=41 align="top">](https://kairollmann.de)
