import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import QueryEditorDropzone from "./QueryEditorDropzone";
import QueryNode from "./QueryNode";
import QueryGroupActions from "./QueryGroupActions";
import type {
  DraggedNodeType,
  DraggedQueryType,
  PreviousQueryQueryNodeType,
  QueryGroupType,
} from "./types";
import { PreviousQueryIdT } from "../previous-queries/list/reducer";
import { DateRangeT } from "../api/types";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
  max-width: 250px;
`;

const Group = styled("div")<{ excluded?: boolean }>`
  position: relative;
  padding: 6px 8px 8px;
  background-color: ${({ theme }) => theme.col.graySuperLight};
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

const isDateActive = (dateRange?: DateRangeT) => {
  return !!dateRange && (!!dateRange.min || !!dateRange.max);
};

interface PropsT {
  group: QueryGroupType;
  andIdx: number;
  onDropNode: (node: DraggedNodeType | DraggedQueryType) => void;
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
  return (
    <Root>
      <QueryEditorDropzone
        key={props.group.elements.length + 1}
        onDropNode={props.onDropNode}
        onDropFile={props.onDropFile}
        onLoadPreviousQuery={props.onLoadPreviousQuery}
      />
      <QueryOrConnector>{T.translate("common.or")}</QueryOrConnector>
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
                {T.translate("common.or")}
              </QueryOrConnector>
            )}
          </div>
        ))}
      </Group>
    </Root>
  );
};

export default QueryGroup;
