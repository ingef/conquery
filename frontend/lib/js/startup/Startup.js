// @flow

import React                from 'react';
import { connect }          from 'react-redux';
import { Route, Switch }    from 'react-router';

import { templates } from '../routes';

import {
  startupOnDataset,
  startupOnQuery,
  loadConfig,
}                           from './actions';

import StartupItem          from './StartupItem';


type PropsType = {
  match: Object,
  history: Object,
  startupOnDataset: Function,
  startupOnQuery: Function,
};

const Startup = (props: PropsType) => (
  <Switch>
    <Route exact path={templates.toDataset}
      render={routeProps =>
        <StartupItem
          {...routeProps}
          onStartup={({datasetId}) => props.startupOnDataset(datasetId)}
        />
      }
    />
    <Route exact path={templates.toQuery}
      render={routeProps =>
        <StartupItem
          {...routeProps}
          onStartup={({datasetId, queryId}) => props.startupOnQuery(datasetId, queryId)}
        />
      }
    />
    <Route exact path="/*"
      render={routeProps =>
        <StartupItem
          {...routeProps}
          onStartup={() => props.startupOnDataset(null)}
        />
      }
    />
  </Switch>
)

const mapStateToProps = (state) => ({});

const mapDispatchToProps = (dispatch) => {
  return {
    startupOnDataset: (datasetId) => {
      dispatch(loadConfig());
      dispatch(startupOnDataset(datasetId));
    },
    startupOnQuery: (datasetId, queryId) => dispatch(startupOnQuery(datasetId, queryId))
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Startup);
