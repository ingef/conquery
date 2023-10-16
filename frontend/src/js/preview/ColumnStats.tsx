import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import { formatCurrency } from "../entity-history/timeline/util";

import { ColumnDescriptionType } from "./Preview";

const Name = styled("code")`
  display: block;
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.xs};
  max-width: 200px;
`;
const Label = styled("span")`
  font-style: italic;
`;
const Values = styled("div")`
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0px 10px;
  font-size: ${({ theme }) => theme.font.xs};
`;
const Value = styled("span")`
  font-weight: 700;
  text-align: right;
`;
interface Props {
  colName: string;
  columnType: ColumnDescriptionType;
  rawColumnData: string[];
}

// Might come in handy at some point
// function getVarianceFromAvg(arr: number[], avg: number) {
//   const diffs = arr.map((val) => Math.abs(avg - val));
//   const sumDiffs = diffs.reduce((a, b) => a + b, 0);

//   return sumDiffs / arr.length;
// }

function getStdDeviationFromAvg(arr: number[], avg: number) {
  const squareDiffs = arr.map((val) => {
    const diff = Math.abs(avg - val);

    return diff * diff;
  });

  const sumSquareDiffs = squareDiffs.reduce((a, b) => a + b, 0);

  return Math.sqrt(sumSquareDiffs / arr.length);
}

function getMedian(sortedArr: number[]) {
  if (sortedArr.length === 0) return 0;

  const half = Math.floor(sortedArr.length / 2);

  return sortedArr.length % 2 === 1
    ? sortedArr[half]
    : (sortedArr[half - 1] + sortedArr[half]) / 2.0;
}

function toRoundedDecimalsString(num: number, decimals: number) {
  const factor = Math.pow(10, decimals);
  const rounded = Math.round(num * factor) / factor;

  return rounded.toFixed(decimals).replace(".", ",");
}

const ColumnStats: FC<Props> = ({ colName, columnType, rawColumnData }) => {
  const { t } = useTranslation();

  switch (columnType) {
    case "NUMERIC":
    case "MONEY":
    case "INTEGER": {
      const cleanSortedData = rawColumnData
        .slice(1)
        .map((x) => {
          if (!x) return 0;

          switch (columnType) {
            case "INTEGER":
              return parseInt(x);
            case "NUMERIC":
            case "MONEY":
            default:
              return parseFloat(x);
          }
        })
        .sort((a, b) => a - b);

      const sum = cleanSortedData.reduce((a, b) => a + b, 0);
      const median = getMedian(cleanSortedData);
      const avg = sum / cleanSortedData.length;
      const min = cleanSortedData[0];
      const max = cleanSortedData[cleanSortedData.length - 1];
      const std = getStdDeviationFromAvg(cleanSortedData, avg);
      const decimals = 2;
      // Might come in handy at some point
      // const variance = getVarianceFromAvg(cleanSortedData, avg);
      const formatValue = (
        num: number,
        { alwaysDecimals }: { alwaysDecimals?: boolean } = {},
      ) => {
        return columnType === "MONEY"
          ? formatCurrency(num / 100, decimals)
          : alwaysDecimals
          ? toRoundedDecimalsString(num, decimals)
          : num;
      };

      return (
        <>
          <Name>{colName}</Name>
          <Values>
            <Label>{t("common.average")}:</Label>
            <Value>{formatValue(avg, { alwaysDecimals: true })}</Value>
            <Label>{t("common.median")}:</Label>
            <Value>{formatValue(median)}</Value>
            <Label>{t("common.min")}:</Label>
            <Value>{formatValue(min)}</Value>
            <Label>{t("common.max")}:</Label>
            <Value>{formatValue(max)}</Value>
            <Label>{t("common.std")}:</Label>
            <Value>{formatValue(std, { alwaysDecimals: true })}</Value>
            <Label>{t("common.sum")}:</Label>
            <Value>{formatValue(sum)}</Value>
          </Values>
        </>
      );
    }
    default:
      return null;
  }
};

export default ColumnStats;
