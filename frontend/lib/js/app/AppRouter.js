// @flow

import React                from 'react';
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

const AppWithAuthToken = WithAuthToken(App);

const AppRouter = (props: PropsType) => {
  return (
    <Router history={props.history}>
      <Switch>
        <Route path="/unauthorized" component={Unauthorized} />
        <Route path="/*" component={AppWithAuthToken} />
      </Switch>
    </Router>
  );
};

export default AppRouter;
