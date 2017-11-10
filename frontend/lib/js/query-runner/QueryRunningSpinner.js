// @flow

import React from 'react';

import spinner from '../../../app/images/spinner.png'; //TODO

type PropsType = {
  isQueryRunning: boolean
};

const QueryRunningSpinner = (props: PropsType) => (
  props.isQueryRunning
    ? <div className="query-runner__results-loading">
        <img src={spinner} className="query-runner__spinner" />
      </div>
    : null
);

export default QueryRunningSpinner;
