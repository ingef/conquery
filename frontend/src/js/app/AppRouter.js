// @flow

import React                from 'react';
import { connect }          from 'react-redux';
import {
  Route,
  Switch,
  Router,
}                           from 'react-router';

import {
  Unauthorized,
  WithAuthToken
}                           from '../authorization'

import App                  from './App';


type PropsType = {
  history: Object
};

const AppRouter = (props: PropsType) => {
  return (
    <Router history={props.history}>
      <Switch>
        <Route path="/unauthorized" component={Unauthorized} />
        <Route path="/*" component={WithAuthToken(App)} />
      </Switch>
    </Router>
  );
};

export default AppRouter;
