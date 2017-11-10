// @flow

import React                from 'react';
import {
  Router,
  Route,
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
      <Route path="/" component={WithAuthToken(App)} />
      <Route path="/unauthorized" component={Unauthorized} />
    </Router>
  );
};

export default AppRouter;
