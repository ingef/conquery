import { faCaretDown, faCaretRight } from "@fortawesome/free-solid-svg-icons";
import { Fragment, memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type {
  ColumnDescriptionSemanticConceptColumn,
  TimeStratifiedInfo,
} from "../../api/types";
import { exists } from "../../common/helpers/exists";
import { getConceptById } from "../../concept-trees/globalTreeStoreHelper";
import FaIcon from "../../icon/FaIcon";
import WithTooltip from "../../ui-components/WithTooltip";
import { ConceptBubble } from "../ConceptBubble";

import { SmallHeading } from "./SmallHeading";
import { formatCurrency, isConceptColumn, isMoneyColumn } from "./util/util";

const stickyWrap = tv({
  base: [
    "sticky top-0 left-0",
    "px-[10px] py-[6px]",
    "cursor-pointer",
    "grid grid-cols-[16px_1fr]",
    "gap-x-0 gap-y-2",
    "rounded",
    "border border-transparent hover:border-primary-200",
  ],
});

const infoGrid = tv({
  base: [
    "grid grid-cols-[auto_minmax(min-content,25px)]",
    "gap-x-[10px] gap-y-0",
  ],
});

const conceptRow = tv({
  base: ["col-span-2", "flex flex-wrap items-center", "gap-1"],
});

// named valueCell: `value` is shadowed by destructured data entries below
const valueCell = tv({
  base: ["text-sm", "font-normal", "justify-self-end", "w-full", "text-right"],
});

const labelText = tv({
  base: [
    "text-sm",
    "max-w-full",
    "whitespace-nowrap",
    "overflow-hidden",
    "text-ellipsis",
  ],
});

type YearValue = TimeStratifiedInfo["years"][number]["values"][string];
type Column = TimeStratifiedInfo["columns"][number];

const byNumericThenAlphabeticLabel = (
  c1: { label: string },
  c2: { label: string },
) => {
  const n1 = Number(c1.label);
  const n2 = Number(c2.label);
  if (!Number.isNaN(n1) && !Number.isNaN(n2)) {
    return n1 - n2;
  }
  return c1.label.localeCompare(c2.label);
};

const formatValue = (column: Column, value: YearValue) => {
  if (typeof value === "number") {
    return isMoneyColumn(column) ? formatCurrency(value) : Math.round(value);
  }
  if (Array.isArray(value)) {
    return value.join(", ");
  }
  return value;
};

const ConceptValues = ({
  label,
  column,
  values,
}: {
  label: string;
  column: Column;
  values: string[];
}) => {
  const semantic = column.semantics.find(
    (s): s is ColumnDescriptionSemanticConceptColumn =>
      s.type === "CONCEPT_COLUMN",
  );
  const concepts = values
    .map((v) => getConceptById(v, semantic!.concept))
    .filter(exists)
    .sort(byNumericThenAlphabeticLabel);

  return (
    <>
      <div className={labelText()} style={{ gridColumn: "span 2" }}>
        {label}
      </div>
      <div className={conceptRow()}>
        {concepts.map((concept) => (
          <WithTooltip key={concept.label} text={concept.description}>
            <ConceptBubble>{concept.label}</ConceptBubble>
          </WithTooltip>
        ))}
      </div>
    </>
  );
};

const TimeStratifiedInfos = ({
  year,
  timeStratifiedInfos,
}: {
  year: number;
  timeStratifiedInfos: TimeStratifiedInfo[];
}) => {
  const infos = timeStratifiedInfos
    .map((info) => {
      return {
        info,
        yearInfo: info.years.find((i) => i.year === year),
      };
    })
    .filter(
      (
        i,
      ): i is {
        info: TimeStratifiedInfo;
        yearInfo: TimeStratifiedInfo["years"][number];
      } => !!i.yearInfo?.values && Object.entries(i.yearInfo.values).length > 0,
    );

  return (
    <div className="flex flex-col gap-[6px]">
      {infos.map(({ info, yearInfo }) => {
        return (
          <div className={infoGrid()} key={info.label}>
            {Object.entries(yearInfo.values)
              .sort(
                ([l1], [l2]) =>
                  info.columns.findIndex((c) => c.label === l1) -
                  info.columns.findIndex((c) => c.label === l2),
              )
              .map(([label, value]) => {
                const column = info.columns.find((c) => c.label === label);

                if (!column) {
                  return null;
                }

                // TODO: Potentially support single-value concepts
                if (isConceptColumn(column) && Array.isArray(value)) {
                  return (
                    <ConceptValues
                      key={label}
                      label={label}
                      column={column}
                      values={value}
                    />
                  );
                }

                const valueFormatted = formatValue(column, value);

                return (
                  <Fragment key={label}>
                    <div className={labelText()}>{label}</div>
                    <div className={valueCell()} title={String(valueFormatted)}>
                      {valueFormatted}
                    </div>
                  </Fragment>
                );
              })}
          </div>
        );
      })}
    </div>
  );
};

const YearHead = ({
  year,
  totalEvents,
  onClick,
  isOpen,
  timeStratifiedInfos,
}: {
  isOpen: boolean;
  year: number;
  totalEvents: number;
  onClick: () => void;
  timeStratifiedInfos: TimeStratifiedInfo[];
}) => {
  const { t } = useTranslation();

  return (
    <div className="pr-[10px] text-xs">
      {/* biome-ignore lint/a11y/noStaticElementInteractions: TODO make this a button */}
      {/* biome-ignore lint/a11y/useKeyWithClickEvents: TODO make this a button */}
      <div className={stickyWrap()} onClick={onClick}>
        <FaIcon large gray icon={isOpen ? faCaretDown : faCaretRight} />
        <div>
          <SmallHeading>{year}</SmallHeading>
          <div>
            {totalEvents}&nbsp;{t("history.events", { count: totalEvents })}
          </div>
        </div>
        <span />
        <TimeStratifiedInfos
          year={year}
          timeStratifiedInfos={timeStratifiedInfos}
        />
      </div>
    </div>
  );
};

export default memo(YearHead);
