import styled from "@emotion/styled";
import React, { FC } from "react";

import { parseStdDate, getDiffInDays } from "../common/helpers";

import { Cell } from "./Cell";

const Span = styled("div")`
  position: absolute;
  top: 0;
  height: 10px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  margin-right: 10px;
  color: white;
  font-size: ${({ theme }) => theme.font.tiny};
  min-width: 1px;
`;

interface PropsT {
  cell: string;
  minDate: Date;
  dateDiffInDays: number;
}

const DateCell: FC<PropsT> = ({ cell, minDate, dateDiffInDays }) => {
  if (cell.length === 0 || cell === "{}") {
    return <Cell isDates></Cell>;
  }

  return (
    <Cell isDates>
      {cell
        .slice(1, cell.length - 1)
        .split(",")
        .map((dateRange, k) => {
          const s = dateRange.split("/");

          const dateStr1 = s[0].trim();
          const date1 = parseStdDate(dateStr1);

          const dateStr2 = s[1].trim();
          const date2 = parseStdDate(dateStr2);

          const diffWidth = date1 && date2 ? getDiffInDays(date1, date2) : 0;
          const diffLeft = date1 ? getDiffInDays(minDate, date1) : 0;

          const left = (diffLeft / dateDiffInDays) * 100;
          const width = (diffWidth / dateDiffInDays) * 100;

          return (
            <Span key={k} style={{ left: `${left}%`, width: `${width}%` }}>
              {diffWidth}
            </Span>
          );
        })}
    </Cell>
  );
};
export default DateCell;
