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
  grid-template-columns: 1fr 1.618fr;
  gap: 10px;
  padding: 20px;
  background-color: ${({ theme }) => theme.col.bg};
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
`;

const Centered = styled("div")`
  display: flex;
  align-items: flex-start;
`;

const Col = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

const Grid = styled("div")`
  display: grid;
  gap: 0 20px;
  grid-template-columns: auto auto;
`;

const Label = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
`;

const Value = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
  font-weight: 400;
  justify-self: end;
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
    <Col>
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

              const valueFormatted =
                typeof value === "number"
                  ? Math.round(value)
                  : value instanceof Array
                  ? value.join(", ")
                  : value;

              return (
                <Fragment key={label}>
                  <Label>{label}</Label>
                  <Value>
                    {valueFormatted}
                    {columnType === "MONEY" ? " " + currencyUnit : ""}
                  </Value>
                </Fragment>
              );
            })}
          </Grid>
        );
      })}
    </Col>
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
