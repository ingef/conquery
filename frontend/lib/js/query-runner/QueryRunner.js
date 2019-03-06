// @flow

import React from "react";
import styled from '@emotion/styled';

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

const Root = styled("div")`
  margin-top: auto;
  padding: 10px 20px 0 10px;
  border-top: 1px solid ${({ theme }) => theme.col.grayLight};
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 71px;
`;

const Left = styled("div")`
  flex-grow: 1;
`;
const Right = styled("div")`
  flex-grow: 2;
  padding-left: 20px;
`;

const LoadingGroup = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
`;

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
    <Root>
      <Left>
        <QueryRunnerButton
          onClick={btnAction}
          isStartStopLoading={isStartStopLoading}
          isQueryRunning={isQueryRunning}
          disabled={!isButtonEnabled}
        />
      </Left>
      <Right>
        <LoadingGroup>
          <QueryRunningSpinner isQueryRunning={isQueryRunning} />
          <QueryRunnerInfo queryRunner={queryRunner} />
        </LoadingGroup>
        <QueryResults
          resultCount={queryRunner.queryResult.resultCount}
          resultUrl={queryRunner.queryResult.resultUrl}
        />
      </Right>
    </Root>
  );
};

export default QueryRunner;
