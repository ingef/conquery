import React, { FC } from "react";
import { ColumnDescriptionType } from "./Preview";
import styled from "@emotion/styled";

const Stats = styled("div")`
  padding: 0 10px 10px 0;
`;
const Stat = styled("code")`
  display: block;
  margin: 0;
`;
const Name = styled("code")`
  display: block;
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.xs};
  max-width: 130px;
  margin-bottom: 8px;
`;
const Label = styled("span")`
  font-style: italic;
  padding-right: 10px;
`;
const Values = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
`;
const Value = styled("span")`
  font-weight: 700;
`;
interface Props {
  colName: string;
  columnType: ColumnDescriptionType;
  rawColumnData: string[];
}

function getVarianceFromAvg(arr: number[], avg: number) {
  const diffs = arr.map((val) => Math.abs(avg - val));
  const sumDiffs = diffs.reduce((a, b) => a + b, 0);

  return sumDiffs / arr.length;
}

function toRoundedDecimalsString(num: number, decimals: number) {
  const factor = Math.pow(10, decimals);
  const rounded = Math.round(num * factor) / factor;

  return rounded.toFixed(decimals);
}

const ColumnStats: FC<Props> = ({ colName, columnType, rawColumnData }) => {
  switch (columnType) {
    case "NUMERIC":
    case "MONEY":
    case "INTEGER": {
      const cleanData = rawColumnData
        .slice(1)
        .filter((x) => !!x)
        .map((x) => (columnType === "INTEGER" ? parseInt(x) : parseFloat(x)));
      const sum = cleanData.reduce((a, b) => a + b, 0);
      const avg = sum / cleanData.length;
      const min = Math.min(...cleanData);
      const max = Math.max(...cleanData);
      const variance = getVarianceFromAvg(cleanData, avg);

      return (
        <Stats>
          <Name>{colName}</Name>
          <Values>
            <Stat>
              <Label>avg:</Label>
              <Value>{toRoundedDecimalsString(avg, 2)}</Value>
            </Stat>
            <Stat>
              <Label>min:</Label>
              <Value>{min}</Value>
            </Stat>
            <Stat>
              <Label>max:</Label>
              <Value>{max}</Value>
            </Stat>
            <Stat>
              <Label>var:</Label>
              <Value>{toRoundedDecimalsString(variance, 2)}</Value>
            </Stat>
          </Values>
        </Stats>
      );
    }
    case "DATE":
    case "DATE_RANGE":
    case "STRING":
    case "ID":
    case "OTHER":
      return null;
  }
};

export default ColumnStats;
