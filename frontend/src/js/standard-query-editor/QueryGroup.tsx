import styled from "@emotion/styled";
import { useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";

import type { DateRangeT, QueryT } from "../api/types";
import type { PreviousQueryT } from "../previous-queries/list/reducer";
import WithTooltip from "../tooltip/WithTooltip";

import QueryEditorDropzone from "./QueryEditorDropzone";
import QueryGroupActions from "./QueryGroupActions";
import QueryNode from "./QueryNode";
import type { QueryGroupType, StandardQueryNodeT } from "./types";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
  max-width: 250px;
`;

const Group = styled("div")<{ excluded?: boolean }>`
  position: relative;
  padding: 6px 8px 8px;
  background-color: ${({ theme }) => theme.col.bg};
  border: ${({ theme, excluded }) =>
    excluded
      ? `2px solid ${theme.col.red}`
      : `1px solid ${theme.col.grayLight}`};
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.12);
  text-align: center;
  border-radius: ${({ theme }) => theme.borderRadius};
  width: 220px;
`;

const QueryOrConnector = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.gray};
  text-align: center;
`;

// To override tippy here.
// Maybe also possible in another way by adjusting
// QueryEditorDropzone styles
const SxWithTooltip = styled(WithTooltip)`
  display: block !important;
`;

const isDateActive = (dateRange?: DateRangeT) => {
  return !!dateRange && (!!dateRange.min || !!dateRange.max);
};

interface PropsT {
  group: QueryGroupType;
  andIdx: number;
  onDropOrNode: (node: StandardQueryNodeT, andIdx: number) => void;
  onDropFile: (file: File, andIdx: number) => void;
  onImportLines: (lines: string[], andIdx?: number) => void;
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
    (lines: string[]) => onImportLines(lines, andIdx),
    [andIdx, onImportLines],
  );

  return (
    <Root>
      <SxWithTooltip text={t("help.editorDropzoneOr")} lazy>
        <QueryEditorDropzone
          key={group.elements.length + 1}
          onDropNode={onDropNode}
          onDropFile={dropFile}
          onLoadPreviousQuery={onLoadPreviousQuery}
          onImportLines={importLines}
        />
      </SxWithTooltip>
      <QueryOrConnector>{t("common.or")}</QueryOrConnector>
      <Group excluded={group.exclude}>
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
                  <QueryOrConnector key={"last-or"}>
                    {t("common.or")}
                  </QueryOrConnector>
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
      </Group>
    </Root>
  );
};

export default QueryGroup;
