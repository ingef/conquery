// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import { isEmpty } from "../common/helpers";
import type { PropsType } from "./QueryNodeEditor";

import ContentCell from "./ContentCell";

const StyledContentCell = styled(ContentCell)`
  border-left: 1px solid ${({ theme }) => theme.col.grayLight};
  max-width: 200px;
`;

const Description = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.black};
`;

const DescriptionColumn = (props: PropsType) => {
  const { node, editorState } = props;

  const selectedTable =
    node.tables && node.tables[editorState.selectedInputTableIdx];

  return (
    <StyledContentCell headline={T.translate("queryNodeEditor.description")}>
      <Description>
        {selectedTable &&
          editorState.selectedInput &&
          !isEmpty(
            selectedTable.filters[editorState.selectedInput].description
          ) && (
            <span>
              {selectedTable.filters[editorState.selectedInput].description}
            </span>
          )}
        {selectedTable &&
          editorState.selectedInput &&
          isEmpty(
            selectedTable.filters[editorState.selectedInput].description
          ) && (
            <span>{T.translate("queryNodeEditor.noDescriptionProvided")}</span>
          )}
        {editorState.selectedInput === null && (
          <span>{T.translate("queryNodeEditor.selectAFilter")}</span>
        )}
      </Description>
    </StyledContentCell>
  );
};

export default DescriptionColumn;
