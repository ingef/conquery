import React, { useRef, memo, FC } from "react";
import styled from "@emotion/styled";
import { useSelector, useDispatch } from "react-redux";
import T from "i18n-react";

import FaIcon from "../icon/FaIcon";

import { setMessage } from "./actions";
import { StateT } from "app-types";
import { useClickOutside } from "js/common/helpers/useClickOutside";

const Root = styled("div")`
  position: fixed;
  z-index: 10;
  bottom: 20px;
  right: 20px;
  background-color: rgba(0, 0, 0, 0.5);
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
  const messageKey = useSelector<StateT, string | null>(
    (state) => state.snackMessage.messageKey
  );
  const dispatch = useDispatch();
  const resetMessage = () => dispatch(setMessage(null));

  useClickOutside(ref, () => {
    if (messageKey) {
      resetMessage();
    }
  });

  return (
    <div ref={ref}>
      {messageKey && (
        <Root>
          <Relative>
            {T.translate(messageKey)}
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
