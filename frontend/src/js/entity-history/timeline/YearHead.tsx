import styled from "@emotion/styled";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import { SmallHeading } from "./SmallHeading";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
  padding: 0 10px;
`;
const StickyWrap = styled("div")`
  position: sticky;
  top: 0;
  left: 0;
  padding: 5px 0;
  cursor: pointer;
`;

const YearHead = ({
  year,
  totalEvents,
  onClick,
}: {
  year: number;
  totalEvents: number;
  onClick: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <Root>
      <StickyWrap onClick={onClick}>
        <SmallHeading>{year}</SmallHeading>
        <div>
          {totalEvents} {t("history.events", { count: totalEvents })}
        </div>
      </StickyWrap>
    </Root>
  );
};

export default memo(YearHead);
