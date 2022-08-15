import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import { SmallHeading } from "./SmallHeading";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
  padding: 10px;
`;
const StickyWrap = styled("div")`
  position: sticky;
  top: 0;
  left: 0;
`;

export const YearHead = ({
  year,
  totalEvents,
}: {
  year: number;
  totalEvents: number;
}) => {
  const { t } = useTranslation();

  return (
    <Root>
      <StickyWrap>
        <SmallHeading>{year}</SmallHeading>
        <div>
          {totalEvents} {t("history.events", { count: totalEvents })}
        </div>
      </StickyWrap>
    </Root>
  );
};
