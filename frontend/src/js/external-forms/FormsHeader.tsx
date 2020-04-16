import React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";
import { reset } from "redux-form";

import { T } from "../localization";
import IconButton from "../button/IconButton";

const Root = styled("div")`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  width: 100%;
`;

const Headline = styled("h3")`
  font-size: ${({ theme }) => theme.font.sm};
`;

export default connect(
  state => ({
    activeForm: state.externalForms.activeForm
  }),
  dispatch => ({ onClear: form => dispatch(reset(form)) })
)(({ headline, onClear, activeForm }) => (
  <Root>
    <Headline>{headline}</Headline>
    <IconButton
      frame
      regular
      icon="trash-alt"
      onClick={() => onClear(activeForm)}
      title={T.translate("externalForms.common.clear")}
    >
      {T.translate("externalForms.common.clear")}
    </IconButton>
  </Root>
));
