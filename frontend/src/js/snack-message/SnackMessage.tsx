import { faTimes } from "@fortawesome/free-solid-svg-icons";
import { memo, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { useClickOutside } from "../common/helpers/useClickOutside";
import FaIcon from "../icon/FaIcon";

import tw from "tailwind-styled-components";
import { resetMessage as resetMessageAction } from "./actions";
import { SnackMessageStateT } from "./reducer";

const Root = tw("div")<{ $success?: boolean }>`
  fixed
  z-10
  bottom-5
  right-5
  text-white
  flex items-start
  max-w-[500px]
  rounded-lg
  ${({ $success }) =>
    $success ? "bg-primary-500 bg-opacity-90" : "bg-black bg-opacity-75"}
`;

const ClearZone = tw("div")`
  absolute top-3 right-4
  z-[11]
  cursor-pointer
  opacity-80 hover:opacity-100
`;

export const SnackMessage = memo(function SnackMessageComponent() {
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
        <Root $success={type === "success"}>
          <div className="relative py-3 pr-10 pl-5">
            <div dangerouslySetInnerHTML={{ __html: message }} />
            <ClearZone onClick={resetMessage}>
              <FaIcon white large icon={faTimes} />
            </ClearZone>
          </div>
        </Root>
      )}
    </div>
  );
});
