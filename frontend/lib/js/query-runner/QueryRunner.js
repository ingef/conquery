// @flow

import React from "react";

import QueryResults from "./QueryResults";
import QueryRunningSpinner from "./QueryRunningSpinner";
import QueryRunnerInfo from "./QueryRunnerInfo";
import QueryRunnerButton from "./QueryRunnerButton";

type PropsType = {
  queryRunner: Object,
  isQueryRunning: boolean,
  isButtonEnabled: boolean,
  startQuery: Function,
  stopQuery: Function
};

const QueryRunner = (props: PropsType) => {
  const {
    queryRunner,
    startQuery,
    stopQuery,
    isQueryRunning,
    isButtonEnabled
  } = props;

  const btnAction = isQueryRunning ? stopQuery : startQuery;

  const isStartStopLoading =
    queryRunner.startQuery.loading || queryRunner.stopQuery.loading;

  return (
    <div className="query-runner">
      <div className="query-runner__left">
        <QueryRunnerButton
          onClick={btnAction}
          isStartStopLoading={isStartStopLoading}
          isQueryRunning={isQueryRunning}
          disabled={!isButtonEnabled}
        />
      </div>
      <div className="query-runner__right">
        <div className="query-runner__loading-group">
          <QueryRunningSpinner isQueryRunning={isQueryRunning} />
          <QueryRunnerInfo queryRunner={queryRunner} />
        </div>
        <QueryResults
          resultCount={queryRunner.queryResult.resultCount}
          resultUrl={queryRunner.queryResult.resultUrl}
        />
      </div>
    </div>
  );
};

export default QueryRunner;
