import { ColumnDescriptionType } from "./Preview";

interface ColumnStats {
  maxDigits?: number;
}

const LETTER_SPACE_IN_PX = 7;

function getMaxDigits(columnIdx: number, data: string[][]) {
  return data.reduce((maxDigits, row) => {
    const split = row[columnIdx].split(",");

    if (split.length === 2) {
      return Math.max(maxDigits, split[1].length);
    }

    return maxDigits;
  }, 0);
}

// To align numbers on comma, we'll first have to get the max digits from the data
export function getStatsByColumn(
  columns: ColumnDescriptionType[],
  data: string[][]
): ColumnStats[] {
  return columns.map((column, columnIdx) => {
    if (column === "MONEY") {
      const maxDigits = getMaxDigits(columnIdx, data);

      return { maxDigits };
    }

    return {};
  });
}

// For a cell and given max digits, we can calculate
// a corrected padding to align the cell on the comma
export function getRightCellPadding(
  cell: string,
  column: ColumnDescriptionType,
  columnStats: ColumnStats
) {
  if (column !== "MONEY") {
    return 0;
  }

  const split = cell.split(",");
  const maxDigits = columnStats.maxDigits || 0;
  const ownDifference =
    split.length === 2 ? maxDigits - split[1].length : maxDigits + 1; // + 1 = the comma;

  return ownDifference * LETTER_SPACE_IN_PX;
}
