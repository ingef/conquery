import styled from "@emotion/styled";
import { useRef, memo, FC } from "react";
import { useSelector, useDispatch } from "react-redux";

import type { StateT } from "../app/reducers";
import { useClickOutside } from "../common/helpers/useClickOutside";
import FaIcon from "../icon/FaIcon";

import { setMessage } from "./actions";
import { SnackMessageStateT, SnackMessageTypeT } from "./reducer";

const colorLookupTable: { [key in SnackMessageTypeT]: string } = {
  [SnackMessageTypeT.ERROR]: "rgba(0, 0, 0, 0.75)",
  [SnackMessageTypeT.SUCCESS]: "rgba(12, 100, 39, 0.75)", // #0C6427
  [SnackMessageTypeT.DEFAULT]: "rgba(0, 0, 0, 0.75)",
};
const Root = styled("div")<{ notificationType: SnackMessageTypeT }>`
  position: fixed;
  z-index: 10;
  bottom: 20px;
  right: 20px;
  background-color: ${({ notificationType }) =>
    colorLookupTable[notificationType ?? SnackMessageTypeT.DEFAULT]};
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
  const { message, notificationType } = useSelector<StateT, SnackMessageStateT>(
    (state) => state.snackMessage,
  );
  const dispatch = useDispatch();
  const resetMessage = () =>
    dispatch(
      setMessage({
        message: null,
        notificationType: SnackMessageTypeT.DEFAULT,
      }),
    );

  useClickOutside(ref, () => {
    if (message) {
      resetMessage();
    }
  });

  return (
    <div ref={ref}>
      {message && (
        <Root notificationType={notificationType}>
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
