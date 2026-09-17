import { faTimes } from "@fortawesome/free-solid-svg-icons";
import { memo, useRef } from "react";
import { Button as RacButton } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { StateT } from "../app/reducers";
import { useClickOutside } from "../common/helpers/useClickOutside";
import { Icon } from "../ui-components/Icon";
import { resetMessage as resetMessageAction } from "./actions";
import type { SnackMessageStateT } from "./reducer";

const root = tv({
  base: [
    "fixed right-5 bottom-5",
    "z-10",
    "flex items-start",
    "max-w-[500px]",
    "rounded-lg",
    "text-white",
  ],
  variants: {
    success: {
      true: "bg-primary-500/90",
      false: "bg-black/75",
    },
  },
});

const clearZone = tv({
  base: [
    "absolute top-3 right-4",
    "z-11",
    "opacity-80 hover:opacity-100",
    "cursor-pointer",
  ],
});

export const SnackMessage = memo(function SnackMessageComponent() {
  const ref = useRef(null);
  const { message, type } = useSelector<StateT, SnackMessageStateT>(
    (state) => state.snackMessage,
  );
  const { t } = useTranslation();
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
        <div className={root({ success: type === "success" })}>
          <div className="relative py-3 pr-10 pl-5">
            {/* biome-ignore lint/security/noDangerouslySetInnerHtml: messages are our own i18n text */}
            <div dangerouslySetInnerHTML={{ __html: message }} />
            <RacButton
              aria-label={t("common.close")}
              className={clearZone()}
              onPress={resetMessage}
            >
              <Icon icon={faTimes} className="text-white" />
            </RacButton>
          </div>
        </div>
      )}
    </div>
  );
});
