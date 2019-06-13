// @flow

import React from "react";
import { T } from "conquery/lib/js/localization";
import styled from "@emotion/styled";
import { connect } from "react-redux";
import { reset } from "redux-form";

import IconButton from "conquery/lib/js/button/IconButton";

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

const ExternalFormsHeader = ({ headline, onClear, activeForm }) => (
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
);

export default connect(
  state => ({
    activeForm: state.externalForms.activeForm
  }),
  dispatch => ({ onClear: form => dispatch(reset(form)) })
)(ExternalFormsHeader);
