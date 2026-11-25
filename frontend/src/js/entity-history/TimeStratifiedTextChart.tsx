import { BanIcon } from "lucide-react";
import { Fragment } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { TimeStratifiedInfo } from "../api/types";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
} from "../ui-components/Tooltip";
import { C2 } from "../ui-components/Typography";

import { ConceptBubble } from "./ConceptBubble";

const container = tv({
  base: [
    "grid place-items-center",
    "gap-x-[3px] gap-y-0",
    "max-w-full",
    "overflow-x-auto",
    "p-[10px]",
  ],
});

const emptyMsg = tv({
  base: ["flex items-center", "gap-[10px]", "my-10", "text-gray-600"],
});

const cell = tv({
  base: ["h-[14px] w-[14px]"],
  variants: {
    filled: {
      true: "bg-primary-200",
      false: "bg-gray-100",
    },
  },
});

const compareValues = (a: string, b: string) => {
  const numericA = Number(a);
  const numericB = Number(b);

  if (!Number.isNaN(numericA) && !Number.isNaN(numericB)) {
    return numericA - numericB;
  }

  return a.localeCompare(b);
};

export const createTimeStratifiedTextMatrix = (
  years: TimeStratifiedInfo["years"],
  columnLabel: string,
) => {
  const sortedYears = [...years].sort((a, b) => b.year - a.year);
  const valuesPerYear = sortedYears.map((year) => {
    const value = year.values[columnLabel];
    if (typeof value === "string") return [value];
    if (Array.isArray(value)) return [...new Set(value)];
    return [];
  });
  const values = [...new Set(valuesPerYear.flat())].sort(compareValues);

  return {
    years: sortedYears.map((year) => year.year),
    values,
    valuesPerYear,
  };
};

export const TimeStratifiedTextChart = ({
  timeStratifiedInfo,
}: {
  timeStratifiedInfo: TimeStratifiedInfo;
}) => {
  const { t } = useTranslation();
  const textColumn = timeStratifiedInfo.columns.find(
    (column) =>
      column.type === "STRING" ||
      (column.type === "LIST[STRING]" &&
        !column.semantics.some(
          (semantic) => semantic.type === "CONCEPT_COLUMN",
        )),
  );

  if (!textColumn) return null;

  const { years, values, valuesPerYear } = createTimeStratifiedTextMatrix(
    timeStratifiedInfo.years,
    textColumn.label,
  );

  if (values.length === 0) {
    return (
      <div className={container()}>
        <div className={emptyMsg()}>
          <BanIcon />
          <C2 as="span">{t("history.noData")}</C2>
        </div>
      </div>
    );
  }

  return (
    <div
      className={container()}
      style={{
        gridTemplateColumns: `repeat(${values.length + 1}, 1fr)`,
      }}
    >
      <div />
      {values.map((value) => (
        <TooltipTrigger key={value}>
          <TooltipTarget as={ConceptBubble} excludeFromTabOrder>
            {value}
          </TooltipTarget>
          <Tooltip>{value}</Tooltip>
        </TooltipTrigger>
      ))}
      {years.map((year, yearIndex) => (
        <Fragment key={year}>
          <C2>{year}</C2>
          {values.map((value) => (
            <div
              key={value}
              className={cell({
                filled: valuesPerYear[yearIndex].includes(value),
              })}
            />
          ))}
        </Fragment>
      ))}
    </div>
  );
};
