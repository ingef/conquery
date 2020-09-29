import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import FaIcon from "../icon/FaIcon";

const ConceptTreesLoading = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 5px 12px;
`;

const StyledFaIcon = styled(FaIcon)`
  margin-right: 10px;
`;

export default () => (
  <ConceptTreesLoading>
    <StyledFaIcon icon="spinner" />
    <span>{T.translate("conceptTreeList.loading")}</span>
  </ConceptTreesLoading>
);
