import styled from "@emotion/styled";
import { FC } from "react";
import Hotkeys from "react-hot-keys";

import { exists } from "../common/helpers/exists";
import Preview from "../preview/Preview";
import WithTooltip from "../tooltip/WithTooltip";

import QueryResults from "./QueryResults";
import QueryRunnerButton from "./QueryRunnerButton";
import QueryRunnerInfo from "./QueryRunnerInfo";
import QueryRunningProgress from "./QueryRunningProgress";
import { QueryRunningSpinner } from "./QueryRunningSpinner";
import type { QueryRunnerStateT } from "./reducer";

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

interface PropsT {
  queryRunner?: QueryRunnerStateT;
  isQueryRunning: boolean;
  isButtonEnabled: boolean;
  buttonTooltip?: string;
  startQuery: () => void;
  stopQuery: () => void;
}

const QueryRunner: FC<PropsT> = ({
  queryRunner,
  startQuery,
  stopQuery,
  buttonTooltip,
  isQueryRunning,
  isButtonEnabled,
}) => {
  const btnAction = isQueryRunning ? stopQuery : startQuery;

  const isStartStopLoading =
    !!queryRunner &&
    !!(queryRunner.startQuery.loading || queryRunner.stopQuery.loading);

  const progress = queryRunner?.progress;

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
        <WithTooltip text={buttonTooltip}>
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
          {exists(progress) && <QueryRunningProgress progress={progress} />}
          {isQueryRunning && <QueryRunningSpinner />}
          {!!queryRunner && <QueryRunnerInfo queryRunner={queryRunner} />}
        </LoadingGroup>
        {!!queryRunner &&
          !!queryRunner.queryResult &&
          !queryRunner.queryResult.error &&
          !queryRunner.queryResult.loading &&
          exists(queryRunner.queryResult.resultUrls) &&
          !isQueryRunning && (
            <QueryResults
              resultCount={queryRunner.queryResult.resultCount}
              resultUrls={queryRunner.queryResult.resultUrls}
              resultColumns={queryRunner.queryResult.resultColumns}
              queryType={queryRunner.queryResult.queryType}
            />
          )}
      </Right>
    </Root>
  );
};

export default QueryRunner;
