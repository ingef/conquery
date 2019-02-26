// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { connect } from "react-redux";
import IconButton from "../button/IconButton";
import { toggleDisplayTooltip } from "./actions";

type PropsType = {
  toggleDisplayTooltip: () => void
};

const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 20px;
  border-top: 0;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
`;

const ActivateTooltip = (props: PropsType) => {
  return (
    <div className="tooltip tooltip--activate">
      <StyledIconButton
        frame
        icon="angle-up"
        onClick={props.toggleDisplayTooltip}
      >
        {T.translate("tooltip.show")}
      </StyledIconButton>
    </div>
  );
};

const mapDispatchToProps = dispatch => ({
  toggleDisplayTooltip: () => dispatch(toggleDisplayTooltip())
});

export default connect(
  () => ({}),
  mapDispatchToProps
)(ActivateTooltip);
