// @flow

import * as React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { connect } from "react-redux";

import { FixedSizeList as List } from "react-window";
import AutoSizer from "react-virtualized-auto-sizer";

import {
  getDiffInDays,
  parseStdDate,
  formatStdDate
} from "../common/helpers/dateHelper";
import useEscPress from "../hooks/useEscPress";

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
  padding: 60px 20px 0;
  z-index: 2;
  overflow-x: auto;
  overflow-y: auto;
`;

const TopRow = styled("div")`
  margin: 0 0 20px;
  width: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
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
  margin: 0;
  display: flex;
  width: 100%;
  align-items: center;
  line-height: 10px;
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
  position: relative;
`;

const Span = styled("div")`
  position: absolute;
  top: 0;
  left: ${({ left }) => `${left}%`};
  width: ${({ width }) => `${width}%`};
  height: 10px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  margin-right: 10px;
  color: white;
  text-shadow: 0 0 1px rgba(0, 0, 0, 0.2);
  font-size: ${({ theme }) => theme.font.tiny};
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

// TODO: Use this to spread dates visualization correctly
function getMinMaxDates(rows: string[][], columns: string[]) {
  let min = null;
  let max = null;

  const dateColumn = columns.find(col => col === "DATE_RANGE");
  const dateColumnIdx = columns.indexOf(dateColumn);

  if (dateColumnIdx === -1) return {};

  for (let row of rows) {
    // To cut off '{' and '}'
    const dateCol = row[dateColumnIdx];
    const dateColTrimmed = dateCol.slice(1, dateCol.length - 1);
    const ranges = dateColTrimmed.split(",");
    const first = parseStdDate(ranges[0].split("/")[0]);
    const last = parseStdDate(ranges[ranges.length - 1].split("/")[1]);

    if (!min || first < min) {
      min = first;
    }
    if (!max || last > max) {
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
  useEscPress(onClose);

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
                    <Span key={k} left={left} width={width}>
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
      <TopRow>
        <StatsRow>
          <Stat>Total: {csv.length}</Stat>
          <Stats>
            <Stat>Min: {formatStdDate(min)}</Stat>
            <Stat>Max: {formatStdDate(max)}</Stat>
          </Stats>
        </StatsRow>
        <TransparentButton icon="times" onClick={onClose}>
          {T.translate("common.done")}
        </TransparentButton>
      </TopRow>
      <Line>
        {slice[0].map((cell, k) => (
          <Cell key={k} isHeader={true}>
            {cell}
          </Cell>
        ))}
      </Line>
      <AutoSizer>
        {({ width, height }) => (
          <List
            height={height}
            width={width}
            itemCount={slice.length}
            itemSize={10}
          >
            {Row}
          </List>
        )}
      </AutoSizer>
    </Preview>
  );
});
