import * as React from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/react";
import { useSelector, useDispatch } from "react-redux";
import Hotkeys from "react-hot-keys";
import T from "i18n-react";

import { getDiffInDays, parseStdDate } from "../common/helpers/dateHelper";

import type { PreviewStateT } from "./reducer";
import { closePreview } from "./actions";
import { StateT } from "app-types";
import type { ColumnDescription, ColumnDescriptionKind } from "../api/types";
import DateCell from "./DateCell";
import { Cell } from "./Cell";
import PreviewInfo from "./PreviewInfo";
import { StatsHeadline } from "./StatsHeadline";
import StatsSubline from "./StatsSubline";

const Root = styled("div")`
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

const Line = styled("div")<{ isHeader?: boolean }>`
  display: flex;
  width: 100%;
  align-items: center;
  line-height: 10px;

  ${({ isHeader }) =>
    isHeader &&
    css`
      border-bottom: "1px solid #ccc";
      align-items: flex-end;
      margin: "0 0 10px";
    `};
`;

const CSVFrame = styled("div")`
  flex-grow: 1;
  overflow: hidden;
  padding: 10px;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
`;

const ScrollWrap = styled("div")`
  overflow: auto;
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const List = styled("div")`
  position: relative;
  height: 100%;
  flex-grow: 1;
`;

export type ColumnDescriptionType = ColumnDescriptionKind | "OTHER";

const SUPPORTED_COLUMN_DESCRIPTION_KINDS = new Set<ColumnDescriptionKind>([
  "ID",
  "INTEGER",
  "NUMERIC",
  "MONEY",
  "DATE",
  "DATE_RANGE",
  "STRING",
  "CATEGORICAL",
  "RESOLUTION",
]);

function detectColumnType(
  cell: string,
  resultColumns: ColumnDescription[]
): ColumnDescriptionType {
  if (cell === "dates") return "DATE_RANGE";

  const maybeColumn = resultColumns.find((column) => column.label === cell);

  if (maybeColumn && SUPPORTED_COLUMN_DESCRIPTION_KINDS.has(maybeColumn.type)) {
    return maybeColumn.type;
  }

  return "OTHER";
}

function detectColumnTypesByHeader(
  line: string[],
  resultColumns: ColumnDescription[]
) {
  return line.map((cell) => detectColumnType(cell, resultColumns));
}

function getFirstAndLastDateOfRange(dateStr: string) {
  const dateStrTrimmed = dateStr.slice(1, dateStr.length - 1);

  const ranges = dateStrTrimmed.split(",");
  const first = parseStdDate(ranges[0].split("/")[0]);
  const last = parseStdDate(ranges[ranges.length - 1].split("/")[1]);

  return { first, last };
}

function getMinMaxDates(
  rows: string[][],
  columns: string[]
): {
  min: Date | null;
  max: Date | null;
  diff: number;
} {
  let min = null;
  let max = null;

  const dateColumn = columns.find((col) => col === "DATE_RANGE");
  const dateColumnIdx = dateColumn ? columns.indexOf(dateColumn) : -1;

  if (dateColumnIdx === -1) return { min: null, max: null, diff: 0 };

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
    diff: min && max ? getDiffInDays(min, max) : 0,
  };
}

const Preview: React.FC = () => {
  const preview = useSelector<StateT, PreviewStateT>((state) => state.preview);
  const dispatch = useDispatch();

  const onClose = () => dispatch(closePreview());

  if (!preview.csv || !preview.resultColumns) return null;

  // Limit size:
  const RENDER_ROWS_LIMIT = 500;
  const previewData = preview.csv.slice(0, RENDER_ROWS_LIMIT + 1); // +1 Header row

  if (previewData.length < 2) return null;

  const columns = detectColumnTypesByHeader(
    previewData[0],
    preview.resultColumns
  );

  const { min, max, diff } = getMinMaxDates(previewData.slice(1), columns);

  const Row = ({ index }: { index: number }) => (
    <Line key={index}>
      {previewData[index + 1].map((cell, j) => {
        if (columns[j] === "DATE_RANGE" && min && max) {
          return (
            <DateCell cell={cell} key={j} minDate={min} dateDiffInDays={diff} />
          );
        }

        if (columns[j] === "MONEY") {
          const cellAsCents = parseInt(cell);

          return (
            <Cell
              title={cell}
              key={j}
              style={{
                textAlign: "right",
              }}
            >
              {isNaN(cellAsCents)
                ? cell
                : (cellAsCents / 100).toFixed(2).replace(".", ",")}
            </Cell>
          );
        }

        return (
          <Cell title={cell} key={j}>
            {cell}
          </Cell>
        );
      })}
    </Line>
  );

  return (
    <Root>
      <Hotkeys keyName="escape" onKeyDown={onClose} />
      <PreviewInfo
        rawPreviewData={preview.csv}
        columns={columns}
        onClose={onClose}
        minDate={min}
        maxDate={max}
      />
      <StatsHeadline>{T.translate("preview.previewHeadline")}</StatsHeadline>
      <StatsSubline>
        {T.translate("preview.previewSubline", { count: RENDER_ROWS_LIMIT })}
      </StatsSubline>
      <CSVFrame>
        <ScrollWrap>
          <Line isHeader>
            {previewData[0].map((cell, k) => (
              <Cell
                isHeader
                key={k}
                title={cell}
                isDates={columns[k] === "DATE_RANGE"}
              >
                {cell}
              </Cell>
            ))}
          </Line>
          <List>
            {previewData.slice(1).map((_, i) => (
              <Row key={i} index={i} />
            ))}
          </List>
        </ScrollWrap>
      </CSVFrame>
    </Root>
  );
};

export default Preview;
