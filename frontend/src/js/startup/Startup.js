// @flow

import React                from 'react';
import { connect }          from 'react-redux';
import { Route, Switch }    from 'react-router';

import { templates } from '../routes';

import {
  startupOnDataset,
  startupOnQuery,
}                           from './actions'


type StartupPropsType = {
  history: Object,
  match: Object,
  location: Object,
  onStartup: Function,
};

class Startup extends React.Component {
  props: StartupPropsType;

  componentDidMount() {
    // Ignore location changes that were triggered by the application
    if (this.props.history.action !== 'REPLACE') {
      const { match } = this.props;
      const params = match && match.params ? match.params : {};
      this.props.onStartup(params);
    }
  }

  render() { return null; }
}


type PropsType = {
  match: Object,
  history: Object,
  startupOnDataset: Function,
  startupOnQuery: Function,
};

const StartupSwitch = (props: PropsType) => (
  <Switch>
    <Route exact path={templates.toDataset}
      render={routeProps =>
        <Startup {...routeProps}
          onStartup={({datasetId}) => props.startupOnDataset(datasetId)} />} />
    <Route exact path={templates.toQuery}
      render={routeProps =>
        <Startup {...routeProps}
          onStartup={({datasetId, queryId}) => props.startupOnQuery(datasetId, queryId)} />} />
    <Route exact path="/*"
      render={routeProps =>
        <Startup {...routeProps} onStartup={() => props.startupOnDataset(null)} />} />
  </Switch>
)

const mapStateToProps = (state) => ({});

const mapDispatchToProps = (dispatch) => {
  return {
    startupOnDataset: (datasetId) => dispatch(startupOnDataset(datasetId)),
    startupOnQuery: (datasetId, queryId) => dispatch(startupOnQuery(datasetId, queryId))
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(StartupSwitch);
