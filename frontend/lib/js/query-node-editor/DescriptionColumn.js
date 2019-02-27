// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import { isEmpty } from "../common/helpers";
import type { PropsType } from "./QueryNodeEditor";

import ColumnWithContent from "./ColumnWithContent";

const StyledColumn = styled(ColumnWithContent)`
  border-left: 1px solid ${({ theme }) => theme.col.grayLight};
  max-width: 200px;
`;

export const DescriptionColumn = (props: PropsType) => {
  const { node, editorState } = props;

  const selectedTable = node.tables[editorState.selectedInputTableIdx];

  return (
    <StyledColumn headline={T.translate("queryNodeEditor.description")}>
      <div className="query-node-editor__description">
        {selectedTable != null &&
          editorState.selectedInput != null &&
          !isEmpty(
            selectedTable.filters[editorState.selectedInput].description
          ) && (
            <span>
              {selectedTable.filters[editorState.selectedInput].description}
            </span>
          )}
        {selectedTable != null &&
          editorState.selectedInput != null &&
          isEmpty(
            selectedTable.filters[editorState.selectedInput].description
          ) && (
            <span>{T.translate("queryNodeEditor.noDescriptionProvided")}</span>
          )}
        {editorState.selectedInput == null && (
          <span>{T.translate("queryNodeEditor.selectAFilter")}</span>
        )}
      </div>
    </StyledColumn>
  );
};
