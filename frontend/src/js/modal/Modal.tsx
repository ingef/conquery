import { type ReactNode, useRef } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { useClickOutside } from "../common/helpers/useClickOutside";
import { Heading3 } from "../headings/Headings";
import { Button } from "../ui-components/Button";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

const root = tv({
  base: [
    "fixed top-0 left-0",
    "z-10",
    "w-full max-w-full",
    "h-screen",
    "flex items-center justify-center",
    "bg-white/50",
    "cursor-pointer",
  ],
});

const modalContent = tv({
  base: [
    "text-left",
    "cursor-[initial]",
    "bg-white",
    "shadow-[0_0_15px_0_rgba(0,0,0,0.2)]",
    "rounded",
    "p-[30px]",
    "mx-5",
    "relative",
    "max-h-[95%]",
  ],
  variants: {
    scrollable: {
      true: "overflow-y-auto",
      false: "overflow-y-visible",
    },
  },
});

const subtitle = tv({ base: ["-mt-[15px] mb-5", "max-w-[600px]"] });

const ModalContent = ({
  children,
  scrollable,
  onClose,
}: {
  children: ReactNode;
  onClose: () => void;
  scrollable?: boolean;
}) => {
  const ref = useRef(null);

  useClickOutside(ref, onClose);

  return (
    <div className={modalContent({ scrollable: !!scrollable })} ref={ref}>
      {children}
    </div>
  );
};

// A modal with two ways to close it
// - click outside
// - press esc
const Modal = ({
  className,
  children,
  headline,
  subtitle: subtitleProp,
  doneButton,
  scrollable,
  dataTestId,
  onClose,
}: {
  className?: string;
  children: ReactNode;
  headline?: ReactNode;
  subtitle?: ReactNode;
  doneButton?: boolean;
  scrollable?: boolean;
  dataTestId?: string;
  onClose: () => void;
}) => {
  const { t } = useTranslation();

  useHotkeys("esc", onClose);

  return (
    <div className={root({ className })} data-test-id={dataTestId}>
      <ModalContent onClose={onClose} scrollable={scrollable}>
        <div className="flex items-start justify-between">
          <Heading3>{headline}</Heading3>
          {doneButton && (
            <TooltipTrigger>
              <Button intent="secondary" onPress={onClose}>
                {t("common.done")}
              </Button>
              <Tooltip>{t("common.closeEsc")}</Tooltip>
            </TooltipTrigger>
          )}
        </div>
        {subtitleProp && <p className={subtitle()}>{subtitleProp}</p>}
        {children}
      </ModalContent>
    </div>
  );
};

export default Modal;
