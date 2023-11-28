import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { FC } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { ColumnDescription, ColumnDescriptionKind } from "../api/types";
import type { StateT } from "../app/reducers";
import {
  getDiffInDays,
  getFirstAndLastDateOfRange,
} from "../common/helpers/dateHelper";

import { Cell } from "./Cell";
import DateCell from "./DateCell";
import PreviewInfo from "./PreviewInfo";
import { StatsHeadline } from "./StatsHeadline";
import StatsSubline from "./StatsSubline";
import { closePreview } from "./actions";
import type { PreviewStateT } from "./reducer";

const FullScreen = styled("div")`
  height: 100%;
  width: 100%;
  position: fixed;
  top: 0;
  left: 0;
  background-color: ${({ theme }) => theme.col.bgAlt};
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
  background-color: white;
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
  "BOOLEAN",
  "INTEGER",
  "NUMERIC",
  "MONEY",
  "DATE",
  "DATE_RANGE",
  "LIST[DATE_RANGE]",
  "STRING",
]);

function detectColumnType(
  cell: string,
  resultColumns: ColumnDescription[],
): ColumnDescriptionType {
  if (cell === "dates") return "DATE_RANGE";

  const maybeColumn = resultColumns.find((column) => column.label === cell);

  if (maybeColumn && SUPPORTED_COLUMN_DESCRIPTION_KINDS.has(maybeColumn.type)) {
    if (maybeColumn.type === "LIST[DATE_RANGE]") {
      return "DATE_RANGE";
    }
    return maybeColumn.type;
  }

  return "OTHER";
}

function detectColumnTypesByHeader(
  line: string[],
  resultColumns: ColumnDescription[],
) {
  return line.map((cell) => detectColumnType(cell, resultColumns));
}

function getMinMaxDates(
  rows: string[][],
  columns: string[],
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

  for (const row of rows) {
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

const Preview: FC = () => {
  const preview = useSelector<StateT, PreviewStateT>((state) => state.previewV1);
  const dispatch = useDispatch();
  const { t } = useTranslation();

  const onClose = () => dispatch(closePreview());

  useHotkeys("esc", () => {
    onClose();
  });

  if (!preview.data.csv || !preview.data.resultColumns) return null;

  // Limit size:
  const RENDER_ROWS_LIMIT = 500;
  const previewData = preview.data.csv.slice(0, RENDER_ROWS_LIMIT + 1); // +1 Header row

  if (previewData.length < 2) return null;

  const columns = detectColumnTypesByHeader(
    previewData[0],
    preview.data.resultColumns,
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
    <FullScreen>
      <PreviewInfo
        rawPreviewData={preview.data.csv}
        columns={columns}
        onClose={onClose}
        minDate={min}
        maxDate={max}
      />
      <StatsHeadline>{t("preview.previewHeadline")}</StatsHeadline>
      <StatsSubline>
        {t("preview.previewSubline", { count: RENDER_ROWS_LIMIT })}
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
    </FullScreen>
  );
};

export default Preview;
