import styled from "@emotion/styled";
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
  gap: 8px;
  grid-template-columns: 12px 1fr;
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
        <FaIcon gray icon={isOpen ? "caret-down" : "caret-right"} />
        <div>
          <SmallHeading>{year}</SmallHeading>
          <div>
            {totalEvents} {t("history.events", { count: totalEvents })}
          </div>
        </div>
      </StickyWrap>
    </Root>
  );
};

export default memo(YearHead);
