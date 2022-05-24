import styled from "@emotion/styled";
import { useRef, FC, ReactNode } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";

import { TransparentButton } from "../button/TransparentButton";
import { useClickOutside } from "../common/helpers/useClickOutside";
import FaIcon from "../icon/FaIcon";
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

const Headline = styled("h3")`
  margin: 0 10px 15px 0;
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme }) => theme.col.blueGrayDark};
`;

const Subtitle = styled(`p`)`
  margin: -15px 0 20px;
`;

const ModalContent: FC<{ onClose: () => void; scrollable?: boolean }> = ({
  children,
  scrollable,
  onClose,
}) => {
  const ref = useRef(null);

  useClickOutside(ref, onClose);

  return (
    <Content scrollable={scrollable} ref={ref}>
      {children}
    </Content>
  );
};

interface PropsT {
  className?: string;
  headline?: ReactNode;
  subtitle?: ReactNode;
  doneButton?: boolean;
  closeIcon?: boolean;
  scrollable?: boolean;
  onClose: () => void;
}

// A modal with three ways to close it
// - a button
// - click outside
// - press esc
const Modal: FC<PropsT> = ({
  className,
  children,
  headline,
  subtitle,
  doneButton,
  closeIcon,
  scrollable,
  onClose,
}) => {
  const { t } = useTranslation();

  useHotkeys("esc", onClose);

  return (
    <Root className={className}>
      <ModalContent onClose={onClose} scrollable={scrollable}>
        <TopRow>
          <Headline>{headline}</Headline>
          {closeIcon && (
            <WithTooltip text={t("common.closeEsc")}>
              <TransparentButton small onClick={onClose}>
                <FaIcon icon="times" />
              </TransparentButton>
            </WithTooltip>
          )}
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
