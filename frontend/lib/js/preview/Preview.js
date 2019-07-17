// @flow

import * as React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";

import { getDiffInDays } from "../common/helpers/dateHelper";
import type { StateT as PreviewStateT } from "./reducer";

import { closePreview } from "./actions";
import useEscPress from "../hooks/useEscPress";

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

const Stats = styled("div")`
  margin: 10px 0 20px;
`;

const Line = styled("div")`
  margin: 0;
  display: flex;
  width: 100%;
  align-items: center;
  line-height: 10px;
`;

const Cell = styled("div")`
  padding: 1px;
  font-size: ${({ theme }) => theme.font.xs};
  display: flex;
  align-items: center;
  width: ${({ isDates }) => (isDates ? "auto" : "100px")};
  flex-grow: ${({ isDates }) => (isDates ? "1" : "0")};
  flex-shrink: 0;
  background-color: white;
`;

const Span = styled("div")`
  width: ${({ len }) => len}px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  height: 10px;
  margin-right: 10px;
  color: white;
  font-size: ${({ theme }) => theme.font.xs};
`;

function detectColumn(cell) {
  if (cell === "dates") return "DATE_RANGE";

  return "OTHER";
}

function detectColumnsByHeader(line: string[]) {
  return line.map(detectColumn);
}

// TODO: Use this to spread dates visualization correctly
function getMinMaxDates(rows, columns) {
  let max = null;
  let min = null;

  const dateColumnIdx = columns.find(col => col === "DATE_RANGE");

  if (!dateColumnIdx) return null;

  for (let row in rows) {
    // To cut off '{' and '}'
    const dateCol = row[dateColumnIdx].slice(0, row[dateColumnIdx].length - 1);
    const ranges = dateCol.split(",");
    const first = parseStdDate(ranges[0].split("/")[0]);
    const last = parseStdDate(ranges[ranges.length - 1].split("/")[1]);

    if (first < min) {
      min = first;
    }
    if (last > max) {
      max = last;
    }
  }

  return {
    min,
    max
  };
}

export default connect(
  state => ({ csv: state.preview.csv }),
  dispatch => ({
    onClose: () => dispatch(closePreview())
  })
)(({ csv, onClose }: PropsT) => {
  if (!csv) return null;

  const columns = detectColumnsByHeader(csv[0]);

  useEscPress(onClose);

  return (
    <Preview>
      <Stats>Total: {csv.length}</Stats>
      {csv.slice(0, 1000).map((row, j) => {
        return (
          <Line>
            {row.map((cell, i) => {
              if (j !== 0 && columns[i] === "DATE_RANGE") {
                return (
                  <Cell isDates>
                    {cell
                      .slice(1, cell.length - 1)
                      .split(",")
                      .map(dateRange => {
                        const s = dateRange.split("/");
                        const diff =
                          Math.abs(getDiffInDays(s[0].trim(), s[1].trim())) + 1;

                        return <Span len={diff}>{diff}</Span>;
                      })}
                  </Cell>
                );
              }

              return <Cell>{cell}</Cell>;
            })}
          </Line>
        );
      })}
    </Preview>
  );
});
