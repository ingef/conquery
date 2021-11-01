import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import { DateRangeT } from "../api/types";
import { PreviousQueryIdT } from "../previous-queries/list/reducer";
import WithTooltip from "../tooltip/WithTooltip";

import QueryEditorDropzone from "./QueryEditorDropzone";
import QueryGroupActions from "./QueryGroupActions";
import QueryNode from "./QueryNode";
import type {
  DragItemConceptTreeNode,
  DragItemNode,
  DragItemQuery,
  PreviousQueryQueryNodeType,
  QueryGroupType,
} from "./types";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
  max-width: 250px;
`;

const Group = styled("div")<{ excluded?: boolean }>`
  position: relative;
  padding: 6px 8px 8px;
  background-color: ${({ theme }) => theme.col.bg};
  border: 1px solid
    ${({ theme, excluded }) => (excluded ? theme.col.red : theme.col.grayLight)};
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
  onDropNode: (
    node: DragItemQuery | DragItemNode | DragItemConceptTreeNode,
  ) => void;
  onDropFile: (file: File) => void;
  onDeleteNode: (idx: number) => void;
  onEditClick: (orIdx: number) => void;
  onExcludeClick: () => void;
  onExpandClick: (q: PreviousQueryQueryNodeType) => void;
  onDateClick: () => void;
  onDeleteGroup: () => void;
  onLoadPreviousQuery: (id: PreviousQueryIdT) => void;
  onToggleTimestamps: (orIdx: number) => void;
  onToggleSecondaryIdExclude: (orIdx: number) => void;
}

const QueryGroup = (props: PropsT) => {
  const { t } = useTranslation();

  return (
    <Root>
      <SxWithTooltip text={t("help.editorDropzoneOr")} lazy>
        <QueryEditorDropzone
          key={props.group.elements.length + 1}
          onDropNode={props.onDropNode}
          onDropFile={props.onDropFile}
          onLoadPreviousQuery={props.onLoadPreviousQuery}
        />
      </SxWithTooltip>
      <QueryOrConnector>{t("common.or")}</QueryOrConnector>
      <Group excluded={props.group.exclude}>
        <QueryGroupActions
          excludeActive={!!props.group.exclude}
          dateActive={isDateActive(props.group.dateRange)}
          onExcludeClick={props.onExcludeClick}
          onDeleteGroup={props.onDeleteGroup}
          onDateClick={props.onDateClick}
        />
        {props.group.elements.map((node, orIdx) => (
          <div key={`or-${orIdx}`}>
            <QueryNode
              node={node}
              andIdx={props.andIdx}
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
