import React, { FC } from "react";
import styled from "@emotion/styled";
import Hotkeys from "react-hot-keys";
import T from "i18n-react";

import Preview from "../preview/Preview";
import WithTooltip from "../tooltip/WithTooltip";

import QueryResults from "./QueryResults";
import QueryRunningSpinner from "./QueryRunningSpinner";
import QueryRunnerInfo from "./QueryRunnerInfo";
import QueryRunnerButton from "./QueryRunnerButton";
import type { QueryRunnerStateT } from "./reducer";

interface PropsT {
  queryRunner?: QueryRunnerStateT;
  isQueryRunning: boolean;
  isButtonEnabled: boolean;
  buttonTooltipKey?: string | null;
  startQuery: Function;
  stopQuery: Function;
}

const Root = styled("div")`
  flex-shrink: 0;
  padding: 10px 20px 0 10px;
  border-top: 1px solid ${({ theme }) => theme.col.grayLight};
  display: flex;
  align-items: center;
  width: 100%;
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
  justify-content: flex-end;
`;

const QueryRunner: FC<PropsT> = ({
  queryRunner,
  startQuery,
  stopQuery,
  buttonTooltipKey,
  isQueryRunning,
  isButtonEnabled,
}) => {
  const btnAction = isQueryRunning ? stopQuery : startQuery;

  const isStartStopLoading =
    !!queryRunner &&
    !!(queryRunner.startQuery.loading || queryRunner.stopQuery.loading);

  return (
    <Root>
      <Hotkeys
        keyName="shift+enter"
        onKeyDown={() => {
          if (isButtonEnabled) btnAction();
        }}
      />
      <Preview />
      <Left>
        <WithTooltip
          text={buttonTooltipKey ? T.translate(buttonTooltipKey) : null}
        >
          <QueryRunnerButton
            onClick={btnAction}
            isStartStopLoading={isStartStopLoading}
            isQueryRunning={isQueryRunning}
            disabled={!isButtonEnabled}
          />
        </WithTooltip>
      </Left>
      <Right>
        <LoadingGroup>
          <QueryRunningSpinner isQueryRunning={isQueryRunning} />
          {!!queryRunner && <QueryRunnerInfo queryRunner={queryRunner} />}
        </LoadingGroup>
        {!!queryRunner &&
          !!queryRunner.queryResult &&
          !queryRunner.queryResult.error &&
          !queryRunner.queryResult.loading &&
          !isQueryRunning && (
            <QueryResults
              datasetId={queryRunner.queryResult.datasetId}
              resultCount={queryRunner.queryResult.resultCount}
              resultUrl={queryRunner.queryResult.resultUrl}
              resultColumns={queryRunner.queryResult.resultColumns}
            />
          )}
      </Right>
    </Root>
  );
};

export default QueryRunner;
