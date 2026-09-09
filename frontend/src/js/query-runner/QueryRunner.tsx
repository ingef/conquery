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

// one row of controls, as high as its content. The results can be wider than
// their cell in a narrow pane; they then run under the run button, which
// paints above them
const root = tv({
  base: [
    "grid grid-cols-[auto_minmax(0,1fr)] items-center",
    "gap-x-5",
    "py-[10px] pr-5 pl-[10px]",
    "border-t border-gray-100",
    "bg-bg-50",
  ],
});

const runButton = tv({ base: ["relative", "z-1"] });

const status = tv({
  base: ["flex items-center justify-end", "gap-[10px]", "min-w-0"],
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
      <div className={runButton()}>
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
      <div className={status()}>
        {exists(progress) && <QueryRunningProgress progress={progress} />}
        {isQueryRunning && <QueryRunningSpinner />}
        {!!queryRunner && <QueryRunnerInfo queryRunner={queryRunner} />}
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
