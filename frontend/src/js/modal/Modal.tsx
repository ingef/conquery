import styled from "@emotion/styled";
import { ReactNode, useRef } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";

import { TransparentButton } from "../button/TransparentButton";
import { useClickOutside } from "../common/helpers/useClickOutside";
import { Heading3 } from "../headings/Headings";
import WithTooltip from "../tooltip/WithTooltip";

const Root = styled("div")`
  position: fixed;
  z-index: 10;
  top: 0;
  left: 0;
  width: 100%;
  max-width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
`;

const Content = styled("div")<{ scrollable?: boolean }>`
  text-align: left;
  cursor: initial;
  background-color: white;
  box-shadow: 0 0 15px 0 rgba(0, 0, 0, 0.2);
  border-radius: ${({ theme }) => theme.borderRadius};
  padding: 30px;
  margin: 0 20px;
  position: relative;
  max-height: 95%;
  overflow-y: ${({ scrollable }) => (scrollable ? "auto" : "visible")};
`;

const TopRow = styled("div")`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
`;

const Subtitle = styled(`p`)`
  margin: -15px 0 20px;
  max-width: 600px;
`;

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
    <Content scrollable={scrollable} ref={ref}>
      {children}
    </Content>
  );
};

// A modal with two ways to close it
// - click outside
// - press esc
const Modal = ({
  className,
  children,
  headline,
  subtitle,
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
    <Root className={className} data-test-id={dataTestId}>
      <ModalContent onClose={onClose} scrollable={scrollable}>
        <TopRow>
          <Heading3>{headline}</Heading3>
          {doneButton && (
            <WithTooltip text={t("common.closeEsc")}>
              <TransparentButton small onClick={onClose}>
                {t("common.done")}
              </TransparentButton>
            </WithTooltip>
          )}
        </TopRow>
        {subtitle && <Subtitle>{subtitle}</Subtitle>}
        {children}
      </ModalContent>
    </Root>
  );
};

export default Modal;
