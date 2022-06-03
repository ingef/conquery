import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { Heading3 } from "../headings/Headings";

const Root = styled("div")`
  display: grid;
  gap: 15px;
  background-color: white;
`;
const SxHeading3 = styled(Heading3)`
  flex-shrink: 0;
  margin: 0;
  white-space: nowrap;
`;

const Count = styled(SxHeading3)`
  justify-self: end;
`;
const Text = styled("span")`
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme }) => theme.col.gray};
  text-transform: uppercase;
  font-weight: 300;
`;

const SpecialText = styled("p")<{ zero?: boolean }>`
  margin: 0;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme, zero }) => (zero ? theme.col.red : theme.col.gray)};
  text-transform: uppercase;
  font-weight: 400;
`;

const StatsGrid = styled("div")`
  display: grid;
  gap: 0px 4px;
  grid-template-columns: auto 1fr;
`;

interface Props {
  className?: string;
  idsCount: number;
  markedCount: number;
}
export const NavigationHeader = ({
  className,
  idsCount,
  markedCount,
}: Props) => {
  const { t } = useTranslation();
  const label = useSelector<StateT, string>(
    (state) => state.entityHistory.label,
  );
  return (
    <Root className={className}>
      <div style={{ overflow: "hidden" }}>
        <SxHeading3>{label}</SxHeading3>
        <SpecialText>{t("history.history")}</SpecialText>
      </div>
      <StatsGrid>
        <Count>{idsCount}</Count>
        <Text>{t("tooltip.entitiesFound", { count: idsCount })}</Text>
        <Count>{markedCount}</Count>
        <Text>{t("history.marked", { count: markedCount })}</Text>
      </StatsGrid>
    </Root>
  );
};
