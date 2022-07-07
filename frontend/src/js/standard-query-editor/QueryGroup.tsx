import styled from "@emotion/styled";
import { useCallback } from "react";
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
  onDropConceptListFile: (file: File, andIdx: number) => void;
  onDeleteNode: (idx: number) => void;
  onEditClick: (orIdx: number) => void;
  onExpandClick: (q: QueryT) => void;
  onExcludeClick: (andIdx: number) => void;
  onDateClick: (andIdx: number) => void;
  onDeleteGroup: (andIdx: number) => void;
  onLoadPreviousQuery: (id: PreviousQueryT["id"]) => void;
  onToggleTimestamps: (orIdx: number) => void;
  onToggleSecondaryIdExclude: (orIdx: number) => void;
}

const QueryGroup = ({
  andIdx,
  onExcludeClick,
  onDateClick,
  onDeleteGroup,
  onDropOrNode,
  onDropConceptListFile,
  ...props
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
  const onDropFile = useCallback(
    (file: File) => onDropConceptListFile(file, andIdx),
    [andIdx, onDropConceptListFile],
  );

  return (
    <Root>
      <SxWithTooltip text={t("help.editorDropzoneOr")} lazy>
        <QueryEditorDropzone
          key={props.group.elements.length + 1}
          onDropNode={onDropNode}
          onDropFile={onDropFile}
          onLoadPreviousQuery={props.onLoadPreviousQuery}
        />
      </SxWithTooltip>
      <QueryOrConnector>{t("common.or")}</QueryOrConnector>
      <Group excluded={props.group.exclude}>
        <QueryGroupActions
          excludeActive={!!props.group.exclude}
          dateActive={isDateActive(props.group.dateRange)}
          onExcludeClick={excludeClick}
          onDeleteGroup={deleteGroup}
          onDateClick={dateClick}
        />
        {props.group.elements.map((node, orIdx) => (
          <div key={`or-${orIdx}`}>
            <QueryNode
              node={node}
              andIdx={andIdx}
              orIdx={orIdx}
              onDeleteNode={() => props.onDeleteNode(orIdx)}
              onEditClick={() => props.onEditClick(orIdx)}
              onToggleTimestamps={() => props.onToggleTimestamps(orIdx)}
              onToggleSecondaryIdExclude={() =>
                props.onToggleSecondaryIdExclude(orIdx)
              }
              onExpandClick={props.onExpandClick}
            />
            {orIdx !== props.group.elements.length - 1 && (
              <QueryOrConnector key={"last-or"}>
                {t("common.or")}
              </QueryOrConnector>
            )}
          </div>
        ))}
      </Group>
    </Root>
  );
};

export default QueryGroup;
