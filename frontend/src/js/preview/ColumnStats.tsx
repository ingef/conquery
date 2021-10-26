import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import { ColumnDescriptionType } from "./Preview";

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

function toLocalizedNumberString(num: number) {
  return num.toString().replace(".", ",");
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
      const toMoneyMaybe = (num: number) => {
        return columnType === "MONEY" ? num / 100 : num;
      };

      return (
        <Stats>
          <Name>{colName}</Name>
          <Values>
            <Stat>
              <Label>{t("common.average")}:</Label>
              <Value>
                {toRoundedDecimalsString(toMoneyMaybe(avg), decimals)}
              </Value>
            </Stat>
            <Stat>
              <Label>{t("common.median")}:</Label>
              <Value>{toLocalizedNumberString(toMoneyMaybe(median))}</Value>
            </Stat>
            <Stat>
              <Label>{t("common.min")}:</Label>
              <Value>{toLocalizedNumberString(toMoneyMaybe(min))}</Value>
            </Stat>
            <Stat>
              <Label>{t("common.max")}:</Label>
              <Value>{toLocalizedNumberString(toMoneyMaybe(max))}</Value>
            </Stat>
            <Stat>
              <Label>{t("common.std")}:</Label>
              <Value>
                {toRoundedDecimalsString(toMoneyMaybe(std), decimals)}
              </Value>
            </Stat>
          </Values>
        </Stats>
      );
    }
    default:
      return null;
  }
};

export default ColumnStats;
