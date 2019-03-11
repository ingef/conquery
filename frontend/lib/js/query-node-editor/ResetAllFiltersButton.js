import React from "react";
import T from "i18n-react";
import { nodeHasActiveFilters } from "../model/node";

export default ({ node, onResetAllFilters }) => {
  if (!nodeHasActiveFilters(node)) return null;

  return (
    <div className="query-node-editor__category_action">
      <span
        className="query-node-editor__reset-all"
        onClick={onResetAllFilters}
      >
        <i className="fa fa-undo" /> {T.translate("queryNodeEditor.resetAll")}
      </span>
    </div>
  );
};
