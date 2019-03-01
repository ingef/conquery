// @flow

import React from "react";

type PropsType = {
  isQueryRunning: boolean
};

const QueryRunningSpinner = (props: PropsType) =>
  props.isQueryRunning ? (
    <div className="query-runner__results-loading">
      <div className="query-runner__spinner" />
    </div>
  ) : null;

export default QueryRunningSpinner;
