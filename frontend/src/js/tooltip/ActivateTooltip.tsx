import styled from "@emotion/styled";
import React from "react";
import { useDispatch } from "react-redux";

import IconButton from "../button/IconButton";

import { toggleDisplayTooltip } from "./actions";

const Root = styled("div")`
  position: relative;
  height: 100%;
`;

const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 45px;
  padding: 6px 10px;
  right: 0;
  border-right: 0;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
`;

const ActivateTooltip = () => {
  const dispatch = useDispatch();
  const onToggleTooltip = () => dispatch(toggleDisplayTooltip());

  return (
    <Root>
      <StyledIconButton
        small
        frame
        icon="angle-right"
        onClick={onToggleTooltip}
      />
    </Root>
  );
};

export default ActivateTooltip;
