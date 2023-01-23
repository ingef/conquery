import styled from "@emotion/styled";
import { useRef, memo, FC } from "react";
import { useSelector, useDispatch } from "react-redux";

import type { StateT } from "../app/reducers";
import { useClickOutside } from "../common/helpers/useClickOutside";
import FaIcon from "../icon/FaIcon";

import { resetMessage as resetMessageAction } from "./actions";
import { SnackMessageStateT, SnackMessageType } from "./reducer";

const snackMessageTypeToColor: Record<SnackMessageType, string> = {
  [SnackMessageType.ERROR]: "rgba(0, 0, 0, 0.75)",
  [SnackMessageType.SUCCESS]: "rgba(12, 100, 39, 0.9)", // #0C6427
  [SnackMessageType.DEFAULT]: "rgba(0, 0, 0, 0.75)",
};

const Root = styled("div")<{ type: SnackMessageType }>`
  position: fixed;
  z-index: 10;
  bottom: 20px;
  right: 20px;
  background-color: ${({ type }) =>
    snackMessageTypeToColor[type ?? SnackMessageType.DEFAULT]};
  color: white;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  max-width: 500px;
  border-radius: 5px;
`;

const Relative = styled("div")`
  position: relative;
  padding: 12px 40px 12px 20px;
`;

const ClearZone = styled("div")`
  position: absolute;
  top: 12px;
  right: 18px;
  z-index: 11;
  cursor: pointer;
  opacity: 0.8;
  &:hover {
    opacity: 1;
  }
`;

const SnackMessage: FC = memo(function SnackMessageComponent() {
  const ref = useRef(null);
  const { message, type } = useSelector<StateT, SnackMessageStateT>(
    (state) => state.snackMessage,
  );
  const dispatch = useDispatch();
  const resetMessage = () => dispatch(resetMessageAction());

  useClickOutside(ref, () => {
    if (message) {
      resetMessage();
    }
  });

  return (
    <div ref={ref}>
      {message && (
        <Root type={type}>
          <Relative>
            <div dangerouslySetInnerHTML={{ __html: message }} />
            <ClearZone onClick={resetMessage}>
              <FaIcon white large icon="times" />
            </ClearZone>
          </Relative>
        </Root>
      )}
    </div>
  );
});

export default SnackMessage;
