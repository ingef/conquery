import styled from "@emotion/styled";

import { EntityInfo, TimeStratifiedInfo } from "../api/types";

import EntityInfos from "./EntityInfos";
import { TabbableTimeStratifiedCharts } from "./TabbableTimeStratifiedCharts";
import { isMoneyColumn } from "./timeline/util";

const Container = styled("div")`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 20px 24px;
  background-color: ${({ theme }) => theme.col.bg};
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
  align-items: center;
`;

const Centered = styled("div")`
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 10px;
`;

export const EntityCard = ({
  className,
  infos,
  timeStratifiedInfos,
}: {
  className?: string;
  infos: EntityInfo[];
  timeStratifiedInfos: TimeStratifiedInfo[];
}) => {
  const infosWithOnlyMoneyColumns = timeStratifiedInfos.filter((info) =>
    info.columns.every(isMoneyColumn),
  );

  return (
    <Container className={className}>
      <Centered>
        <EntityInfos infos={infos} />
      </Centered>
      <TabbableTimeStratifiedCharts infos={infosWithOnlyMoneyColumns} />
    </Container>
  );
};
