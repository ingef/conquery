import styled from "@emotion/styled";
import { faAngleRight } from "@fortawesome/free-solid-svg-icons";
import { useDispatch } from "react-redux";

import IconButton from "../button/IconButton";

import { toggleDisplayTooltip } from "./actions";

const Root = styled("div")`
  position: relative;
  height: 100%;
`;

const StyledIconButton = styled(IconButton)`
  position: absolute;
  width: 100%;
  top: 40px;
  bottom: 0;
  right: 0;
  padding: 12px 12px;
  border-radius: 0;
  display: flex;
  align-items: flex-start;
`;

const ActivateTooltip = () => {
  const dispatch = useDispatch();
  const onToggleTooltip = () => dispatch(toggleDisplayTooltip());

  return (
    <Root>
      <StyledIconButton bgHover icon={faAngleRight} onClick={onToggleTooltip} />
    </Root>
  );
};

export default ActivateTooltip;
