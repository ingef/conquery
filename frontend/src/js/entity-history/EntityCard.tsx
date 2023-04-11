import styled from "@emotion/styled";

import { EntityInfo, TimeStratifiedInfo } from "../api/types";

import EntityInfos from "./EntityInfos";

const Container = styled("div")`
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 30px;
  padding: 20px;
  background-color: ${({ theme }) => theme.col.bg};
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
`;

const Centered = styled("div")`
  display: flex;
  align-items: center;
`;

const Grid = styled("div")`
  display: flex;
  gap: 10px;
  flex-direction: column;
`;
const Label = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
`;
const Value = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
  font-weight: 400;
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
  return (
    <Container className={className}>
      <Centered>
        <EntityInfos infos={infos} />
      </Centered>
      {timeStratifiedInfos.map((timeStratifiedInfo) => (
        <Grid>
          {Object.entries(timeStratifiedInfo.totals).map(([k, v]) => (
            <div key={k}>
              <Value>{v}</Value>
              <Label>{k}</Label>
            </div>
          ))}
        </Grid>
      ))}
    </Container>
  );
};
