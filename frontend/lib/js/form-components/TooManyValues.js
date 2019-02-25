import React from "react";
import T from "i18n-react";
import styled from "@emotion/styled";

import CloseIconButton from "../button/CloseIconButton";

const Root = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
`;

export default ({ value, onClear }) => (
  <Root>
    <p>
      {T.translate("queryNodeEditor.tooManyValues", { count: value.length })}
    </p>
    <CloseIconButton
      title={T.translate("common.clearValue")}
      aria-label={T.translate("common.clearValue")}
      onClick={onClear}
    />
  </Root>
);
