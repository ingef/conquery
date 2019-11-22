// @flow

import * as React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { connect } from "react-redux";
import Hotkeys from "react-hot-keys";

import { FixedSizeList as List } from "react-window";
import AutoSizer from "react-virtualized-auto-sizer";

import {
  getDiffInDays,
  parseStdDate,
  formatStdDate,
  formatDateDistance
} from "../common/helpers/dateHelper";

import TransparentButton from "../button/TransparentButton";

import type { StateT as PreviewStateT } from "./reducer";
import { closePreview } from "./actions";

type PropsT = {
  preview: PreviewStateT
};

const Preview = styled("div")`
  height: 100%;
  width: 100%;
  position: fixed;
  top: 0;
  left: 0;
  background-color: white;
  padding: 60px 20px 20px;
  z-index: 2;
  display: flex;
  flex-direction: column;
`;

const TopRow = styled("div")`
  margin: 12px 0 20px;
  width: 100%;
  display: flex;
  align-items: center;
`;

const StatsRow = styled("div")`
  display: flex;
  align-items: center;
`;
const Stats = styled("div")``;
const Stat = styled("code")`
  display: block;
  margin: 0;
  padding-right: 10px;
  font-size: ${({ theme }) => theme.font.xs};
`;

const Line = styled("div")`
  display: flex;
  width: 100%;
  align-items: center;
  line-height: 10px;
  border-bottom: ${({ isHeader }) => (isHeader ? "1px solid #ccc" : "none")};
  margin: ${({ isHeader }) => (isHeader ? "0 0 10px" : "0")};
`;

const Cell = styled("code")`
  padding: 1px 5px;
  font-size: ${({ theme }) => theme.font.xs};
  display: flex;
  align-items: center;
  width: ${({ isDates }) => (isDates ? "auto" : "100px")};
  flex-grow: ${({ isDates }) => (isDates ? "1" : "0")};
  flex-shrink: 0;
  background-color: white;
  overflow-wrap: ${({ isHeader }) => (isHeader ? "break-word" : "normal")};
  font-weight: ${({ isHeader }) => (isHeader ? "700" : "400")};
  margin: ${({ isHeader }) => (isHeader ? "0 0 5px" : "0")};
  position: relative;
`;

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

const Headline = styled("h2")`
  font-size: ${({ theme }) => theme.font.md};
  margin: 0;
`;

const Explanation = styled("p")`
  font-size: ${({ theme }) => theme.font.sm};
  margin: 0;
`;

const HeadInfo = styled("div")`
  margin: 0 20px;
`;

const CSVFrame = styled("div")`
  flex-grow: 1;
  overflow: hidden;
  padding: 10px;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
`;

function detectColumn(cell) {
  if (cell === "dates") return "DATE_RANGE";

  return "OTHER";
}

function detectColumnsByHeader(line: string[]) {
  return line.map(detectColumn);
}

function getDaysDiff(d1, d2) {
  return Math.abs(getDiffInDays(d1, d2)) + 1;
}

function getFirstAndLastDateOfRange(dateStr: string) {
  const dateStrTrimmed = dateStr.slice(1, dateStr.length - 1);

  const ranges = dateStrTrimmed.split(",");
  const first = parseStdDate(ranges[0].split("/")[0]);
  const last = parseStdDate(ranges[ranges.length - 1].split("/")[1]);

  return { first, last };
}

function getMinMaxDates(rows: string[][], columns: string[]) {
  let min = null;
  let max = null;

  const dateColumn = columns.find(col => col === "DATE_RANGE");
  const dateColumnIdx = columns.indexOf(dateColumn);

  if (dateColumnIdx === -1) return {};

  for (let row of rows) {
    // To cut off '{' and '}'
    const cell = row[dateColumnIdx];
    const { first, last } = getFirstAndLastDateOfRange(cell);

    if (!!first && (!min || first < min)) {
      min = first;
    }
    if (!!last && (!max || last > max)) {
      max = last;
    }
  }

  return {
    min,
    max,
    diff: getDaysDiff(min, max)
  };
}

export default connect(
  state => ({ csv: state.preview.csv }),
  dispatch => ({
    onClose: () => dispatch(closePreview())
  })
)(({ csv, onClose }: PropsT) => {
  if (!csv || csv.length < 2) return null;

  const columns = detectColumnsByHeader(csv[0]);

  // Potentially, limit size:
  // const slice = csv.slice(1000);
  const slice = csv.slice();

  const { min, max, diff } = getMinMaxDates(slice.slice(1), columns);

  const Row = ({ index, style }) => (
    <Line style={style} key={index}>
      {slice[index + 1].map((cell, i) => {
        if (columns[i] === "DATE_RANGE") {
          return (
            <Cell key={i} isDates>
              {cell
                .slice(1, cell.length - 1)
                .split(",")
                .map((dateRange, k) => {
                  const s = dateRange.split("/");
                  const dateStr1 = s[0].trim();
                  const date1 = parseStdDate(dateStr1);

                  const dateStr2 = s[1].trim();
                  const date2 = parseStdDate(dateStr2);

                  const diffWidth = getDaysDiff(date1, date2);
                  const diffLeft = getDaysDiff(min, date1);

                  const left = (diffLeft / diff) * 100;
                  const width = (diffWidth / diff) * 100;

                  return (
                    <Span
                      key={k}
                      style={{ left: `${left}%`, width: `${width}%` }}
                    >
                      {diffWidth}
                    </Span>
                  );
                })}
            </Cell>
          );
        }

        return <Cell key={i}>{cell}</Cell>;
      })}
    </Line>
  );

  return (
    <Preview>
      <Hotkeys keyName="escape" onKeyDown={onClose} />
      <TopRow>
        <TransparentButton icon="times" onClick={onClose}>
          {T.translate("common.back")}
        </TransparentButton>
        <HeadInfo>
          <Headline>{T.translate("preview.headline")}</Headline>
          <Explanation>{T.translate("preview.explanation")}</Explanation>
        </HeadInfo>
        <StatsRow>
          <Stats>
            <Stat>
              {T.translate("preview.total")}: {csv.length}
            </Stat>
            <Stat>
              {T.translate("preview.min")}: {formatStdDate(min)}
            </Stat>
            <Stat>
              {T.translate("preview.max")}: {formatStdDate(max)}
            </Stat>
            <Stat>
              {T.translate("preview.span")}: {formatDateDistance(min, max)}
            </Stat>
          </Stats>
        </StatsRow>
      </TopRow>
      <CSVFrame>
        <Line isHeader={true}>
          {slice[0].map((cell, k) => (
            <Cell key={k} isHeader={true} isDates={columns[k] === "DATE_RANGE"}>
              {cell}
            </Cell>
          ))}
        </Line>
        <AutoSizer>
          {({ width, height }) => (
            <List
              height={height}
              width={width}
              itemCount={slice.length - 1}
              itemSize={10}
            >
              {Row}
            </List>
          )}
        </AutoSizer>
      </CSVFrame>
    </Preview>
  );
});
