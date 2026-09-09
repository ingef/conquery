import { useHotkeys } from "react-hotkeys-hook";
import { tv } from "tailwind-variants";

import { exists } from "../common/helpers/exists";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

import QueryResults from "./QueryResults";
import QueryRunnerButton from "./QueryRunnerButton";
import QueryRunnerInfo from "./QueryRunnerInfo";
import QueryRunningProgress from "./QueryRunningProgress";
import { QueryRunningSpinner } from "./QueryRunningSpinner";
import type { QueryRunnerStateT } from "./reducer";

const root = tv({
  base: [
    "flex items-center",
    "w-full",
    "shrink-0",
    "py-[10px] pr-5 pl-[10px]",
    "border-t border-gray-100",
    "bg-bg-50",
  ],
});

const QueryRunner = ({
  queryRunner,
  startQuery,
  stopQuery,
  buttonTooltip,
  isQueryRunning,
  disabled,
}: {
  queryRunner?: QueryRunnerStateT;
  isQueryRunning: boolean;
  disabled: boolean;
  buttonTooltip?: string;
  startQuery: () => void;
  stopQuery: () => void;
}) => {
  const btnAction = isQueryRunning ? stopQuery : startQuery;
  const isStartStopLoading =
    !!queryRunner &&
    !!(queryRunner.startQuery.loading || queryRunner.stopQuery.loading);

  const progress = queryRunner?.progress;

  useHotkeys("shift+enter", () => {
    if (!disabled) btnAction();
  }, [disabled, btnAction]);

  return (
    <div className={root()} data-test-id="query-runner">
      <div className="grow">
        <TooltipTrigger>
          <QueryRunnerButton
            onClick={btnAction}
            isStartStopLoading={isStartStopLoading}
            isQueryRunning={isQueryRunning}
            disabled={disabled}
          />
          <Tooltip>{buttonTooltip}</Tooltip>
        </TooltipTrigger>
      </div>
      <div className="grow-[2] pl-5">
        <div className="flex flex-row items-center justify-end">
          {exists(progress) && <QueryRunningProgress progress={progress} />}
          {isQueryRunning && <QueryRunningSpinner />}
          {!!queryRunner && <QueryRunnerInfo queryRunner={queryRunner} />}
        </div>
        {!!queryRunner &&
          !!queryRunner.queryResult &&
          !queryRunner.queryResult.error &&
          !queryRunner.queryResult.loading &&
          exists(queryRunner.queryResult.resultUrls) &&
          exists(queryRunner.queryResult.resultLabel) &&
          !isQueryRunning && (
            <QueryResults
              resultLabel={queryRunner.queryResult.resultLabel}
              resultCount={queryRunner.queryResult.resultCount}
              resultUrls={queryRunner.queryResult.resultUrls}
              resultColumns={queryRunner.queryResult.resultColumns}
              queryType={queryRunner.queryResult.queryType}
              previewAvailable={queryRunner.queryResult.previewAvailable}
            />
          )}
      </div>
    </div>
  );
};

export default QueryRunner;
