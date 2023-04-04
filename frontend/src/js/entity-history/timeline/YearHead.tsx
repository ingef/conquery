import styled from "@emotion/styled";
import { faCaretDown, faCaretRight } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import FaIcon from "../../icon/FaIcon";

import { SmallHeading } from "./SmallHeading";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
  padding: 0 10px 0 0;
`;
const StickyWrap = styled("div")`
  position: sticky;
  top: 0;
  left: 0;
  padding: 5px 0;
  cursor: pointer;
  display: grid;
  grid-template-columns: 20px 1fr;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid transparent;
  &:hover {
    border: 1px solid ${({ theme }) => theme.col.blueGray};
  }
`;

const YearHead = ({
  year,
  totalEvents,
  onClick,
  isOpen,
}: {
  isOpen: boolean;
  year: number;
  totalEvents: number;
  onClick: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <Root>
      <StickyWrap onClick={onClick}>
        <FaIcon large gray icon={isOpen ? faCaretDown : faCaretRight} />
        <div>
          <SmallHeading>{year}</SmallHeading>
          <div>
            {totalEvents}&nbsp;{t("history.events", { count: totalEvents })}
          </div>
        </div>
      </StickyWrap>
    </Root>
  );
};

export default memo(YearHead);
