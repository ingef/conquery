import styled from "@emotion/styled";
import { Fragment } from "react";
import { useSelector } from "react-redux";

import { EntityInfo, TimeStratifiedInfo } from "../api/types";
import { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";

import EntityInfos from "./EntityInfos";
import { getColumnType } from "./timeline/util";

const Container = styled("div")`
  display: grid;
  grid-template-columns: 1.618fr 1fr;
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
  display: inline-grid;
  gap: 4px 10px;
  grid-template-columns: auto auto;
`;
const Label = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
`;
const Value = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
  font-weight: 400;
`;

const TimeStratifiedInfos = ({
  timeStratifiedInfos,
}: {
  timeStratifiedInfos: TimeStratifiedInfo[];
}) => {
  const currencyUnit = useSelector<StateT, string>(
    (state) => state.startup.config.currency.unit,
  );

  return (
    <div>
      {timeStratifiedInfos.map((timeStratifiedInfo) => {
        return (
          <Grid key={timeStratifiedInfo.label}>
            {timeStratifiedInfo.columns.map((column) => {
              const columnType = getColumnType(
                timeStratifiedInfo,
                column.label,
              );

              const label = column.label;
              const value = timeStratifiedInfo.totals[column.label];

              if (!exists(value)) return <></>;

              return (
                <Fragment key={label}>
                  <Value>
                    {value}
                    {columnType === "MONEY" ? " " + currencyUnit : ""}
                  </Value>
                  <Label>{label}</Label>
                </Fragment>
              );
            })}
          </Grid>
        );
      })}
    </div>
  );
};

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
      <TimeStratifiedInfos timeStratifiedInfos={timeStratifiedInfos} />
    </Container>
  );
};
