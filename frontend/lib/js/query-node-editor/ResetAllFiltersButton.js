import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { nodeHasActiveFilters } from "../model/node";

import IconButton from "../button/IconButton";

const ResetAllFiltersButton = styled("div")`
  text-transform: uppercase;
  padding: 10px 2px;
  white-space: nowrap;
`;

export default ({ node, onResetAllFilters }) => {
  if (!nodeHasActiveFilters(node)) return null;

  return (
    <ResetAllFiltersButton>
      <IconButton active onClick={onResetAllFilters} icon="undo">
        {T.translate("queryNodeEditor.resetSettings")}
      </IconButton>
    </ResetAllFiltersButton>
  );
};
