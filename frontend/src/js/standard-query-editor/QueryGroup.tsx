import { useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { DateRangeT, QueryT } from "../api/types";
import type { PreviousQueryT } from "../previous-queries/list/reducer";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
  tooltipDelay,
} from "../ui-components/Tooltip";

import QueryEditorDropzone from "./QueryEditorDropzone";
import QueryGroupActions from "./QueryGroupActions";
import QueryNode from "./QueryNode";
import type { QueryGroupType, StandardQueryNodeT } from "./types";

const groupBox = tv({
  base: [
    "relative",
    "w-[220px]",
    "pt-[6px] px-2 pb-2",
    "bg-bg-50",
    "shadow-[0_0_10px_0_rgba(0,0,0,0.12)]",
    "rounded",
    "text-center",
  ],
  variants: {
    excluded: {
      true: "border-2 border-red",
      false: "border border-gray-100",
    },
  },
});

const queryOrConnector = tv({
  base: ["text-sm", "text-gray-500", "text-center"],
});

const isDateActive = (dateRange?: DateRangeT) => {
  return !!dateRange && (!!dateRange.min || !!dateRange.max);
};

interface PropsT {
  group: QueryGroupType;
  andIdx: number;
  onDropOrNode: (node: StandardQueryNodeT, andIdx: number) => void;
  onDropFile: (file: File, andIdx: number) => void;
  onImportLines: (lines: string[], filename?: string, andIdx?: number) => void;
  onDeleteNode: (andIdx: number, orIdx: number) => void;
  onEditClick: (andIdx: number, orIdx: number) => void;
  onExpandClick: (q: QueryT) => void;
  onExcludeClick: (andIdx: number) => void;
  onDateClick: (andIdx: number) => void;
  onDeleteGroup: (andIdx: number) => void;
  onLoadPreviousQuery: (id: PreviousQueryT["id"]) => void;
  onToggleTimestamps: (andIdx: number, orIdx: number) => void;
  onToggleSecondaryIdExclude: (andIdx: number, orIdx: number) => void;
}

const QueryGroup = ({
  group,
  andIdx,
  onExcludeClick,
  onDateClick,
  onDeleteGroup,
  onDropOrNode,
  onDropFile,
  onImportLines,
  onDeleteNode,
  onEditClick,
  onExpandClick,
  onToggleTimestamps,
  onToggleSecondaryIdExclude,
  onLoadPreviousQuery,
}: PropsT) => {
  const { t } = useTranslation();

  const onDropNode = useCallback(
    (item: StandardQueryNodeT) => {
      onDropOrNode(item, andIdx);
    },
    [andIdx, onDropOrNode],
  );
  const excludeClick = useCallback(
    () => onExcludeClick(andIdx),
    [andIdx, onExcludeClick],
  );
  const deleteGroup = useCallback(
    () => onDeleteGroup(andIdx),
    [andIdx, onDeleteGroup],
  );
  const dateClick = useCallback(
    () => onDateClick(andIdx),
    [andIdx, onDateClick],
  );
  const dropFile = useCallback(
    (file: File) => onDropFile(file, andIdx),
    [andIdx, onDropFile],
  );
  const importLines = useCallback(
    (lines: string[], filename?: string) =>
      onImportLines(lines, filename, andIdx),
    [andIdx, onImportLines],
  );

  return (
    <div className="max-w-[250px] text-sm">
      {/* block! overrides tippy's inline-block wrapper */}
      <TooltipTrigger delay={tooltipDelay.info}>
        <TooltipTarget as="div" excludeFromTabOrder>
          <QueryEditorDropzone
            key={group.elements.length + 1}
            onDropNode={onDropNode}
            onDropFile={dropFile}
            onLoadPreviousQuery={onLoadPreviousQuery}
            onImportLines={importLines}
          />
        </TooltipTarget>
        <Tooltip>{t("help.editorDropzoneOr")}</Tooltip>
      </TooltipTrigger>
      <p className={queryOrConnector()}>{t("common.or")}</p>
      <div
        className={groupBox({ excluded: !!group.exclude })}
        data-test-id="query-group"
      >
        <QueryGroupActions
          excludeActive={!!group.exclude}
          dateActive={isDateActive(group.dateRange)}
          onExcludeClick={excludeClick}
          onDeleteGroup={deleteGroup}
          onDateClick={dateClick}
        />
        {useMemo(
          () =>
            group.elements.map((node, orIdx) => (
              <div key={`or-${orIdx}`}>
                <QueryNode
                  node={node}
                  andIdx={andIdx}
                  orIdx={orIdx}
                  onDeleteNode={onDeleteNode}
                  onEditClick={onEditClick}
                  onToggleTimestamps={onToggleTimestamps}
                  onToggleSecondaryIdExclude={onToggleSecondaryIdExclude}
                  onExpandClick={onExpandClick}
                />
                {orIdx !== group.elements.length - 1 && (
                  <p className={queryOrConnector()} key={"last-or"}>
                    {t("common.or")}
                  </p>
                )}
              </div>
            )),
          [
            t,
            andIdx,
            group.elements,
            onDeleteNode,
            onEditClick,
            onToggleTimestamps,
            onToggleSecondaryIdExclude,
            onExpandClick,
          ],
        )}
      </div>
    </div>
  );
};

export default QueryGroup;
