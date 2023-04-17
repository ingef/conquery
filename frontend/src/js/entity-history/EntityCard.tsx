import styled from "@emotion/styled";
import { useSelector } from "react-redux";

import { EntityInfo, TimeStratifiedInfo } from "../api/types";
import { StateT } from "../app/reducers";

import EntityInfos from "./EntityInfos";
import { getColumnType } from "./timeline/util";

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
  align-items: flex-start;
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
  const currencyPrefix = useSelector<StateT, string>(
    (state) => state.startup.config.currency.prefix,
  );

  return (
    <Container className={className}>
      <Centered>
        <EntityInfos infos={infos} />
      </Centered>
      {timeStratifiedInfos.map((timeStratifiedInfo) => {
        return (
          <Grid key={timeStratifiedInfo.label}>
            {Object.entries(timeStratifiedInfo.totals).map(([label, value]) => {
              const columnType = getColumnType(timeStratifiedInfo, label);

              return (
                <div key={label}>
                  <Value>
                    {value}
                    {columnType === "MONEY" ? " " + currencyPrefix : ""}
                  </Value>
                  <Label>{label}</Label>
                </div>
              );
            })}
          </Grid>
        );
      })}
    </Container>
  );
};
