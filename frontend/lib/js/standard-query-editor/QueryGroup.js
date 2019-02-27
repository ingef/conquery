// @flow

import React from "react";
import T from "i18n-react";

import { type DraggedNodeType } from "../model/node";

import { QueryEditorDropzone } from "./QueryEditorDropzone";
import QueryNode from "./QueryNode";
import QueryGroupActions from "./QueryGroupActions";
import type { QueryGroupType } from "./types";

type PropsType = {
  group: QueryGroupType,
  andIdx: number,
  onDropNode: DraggedNodeType => void,
  onDropFile: Function,
  onDeleteNode: Function,
  onFilterClick: Function,
  onExcludeClick: Function,
  onExpandClick: Function,
  onDateClick: Function,
  onDeleteGroup: Function,
  onLoadPreviousQuery: Function
};

const QueryGroup = (props: PropsType) => {
  const groupExcludeActiveClass = props.group.exclude
    ? "query-group__group--active"
    : "";

  return (
    <div className="query-group">
      <QueryEditorDropzone
        key={props.group.elements.length + 1}
        onDropNode={props.onDropNode}
        onDropFile={props.onDropFile}
        onLoadPreviousQuery={props.onLoadPreviousQuery}
      />
      <p className="query-or-connector">{T.translate("common.or")}</p>
      <div className={`query-group__group ${groupExcludeActiveClass}`}>
        <QueryGroupActions
          excludeActive={props.group.exclude}
          dateActive={!!props.group.dateRange}
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
              onFilterClick={() => props.onFilterClick(orIdx)}
              onExpandClick={props.onExpandClick}
            />
            {orIdx !== props.group.elements.length - 1 && (
              <p key={"last-or"} className="query-or-connector">
                {T.translate("common.or")}
              </p>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default QueryGroup;
