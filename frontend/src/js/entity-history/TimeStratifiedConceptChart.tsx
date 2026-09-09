import { faBan } from "@fortawesome/free-solid-svg-icons";
import { Fragment } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type {
  ColumnDescriptionSemanticConceptColumn,
  TimeStratifiedInfo,
} from "../api/types";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import FaIcon from "../icon/FaIcon";
import WithTooltip from "../ui-components/WithTooltip";

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
  base: [
    "flex items-center",
    "gap-[10px]",
    "my-10",
    "text-base",
    "text-gray-500",
  ],
});

const bubble = tv({
  base: ["h-[14px] w-[14px]"],
  variants: {
    filled: {
      true: "bg-primary-200",
      false: "bg-gray-100",
    },
  },
});

export const TimeStratifiedConceptChart = ({
  timeStratifiedInfo,
}: {
  timeStratifiedInfo: TimeStratifiedInfo;
}) => {
  const { t } = useTranslation();
  const conceptColumn = timeStratifiedInfo.columns.at(-1);

  if (!conceptColumn) return null;

  const conceptSemantic = conceptColumn.semantics.find(
    (s): s is ColumnDescriptionSemanticConceptColumn =>
      s.type === "CONCEPT_COLUMN",
  );

  if (!conceptSemantic) return null;

  const descYearInfos = [...timeStratifiedInfo.years].sort(
    (a, b) => b.year - a.year,
  );

  const years = descYearInfos.map((y) => y.year);
  const valuesPerYear = descYearInfos.map((y) =>
    ((y.values[Object.keys(y.values)[0]] as string[]) || []).map(
      (conceptId) => getConceptById(conceptId, conceptSemantic?.concept)!,
    ),
  );

  const allValues = [
    ...new Set(
      valuesPerYear.flat().sort((a, b) => {
        const nA = Number(a?.label);
        const nB = Number(b?.label);
        if (!Number.isNaN(nA) && !Number.isNaN(nB)) return nA - nB;
        return a?.label.localeCompare(b?.label);
      }),
    ),
  ];

  if (allValues.length === 0)
    return (
      <div className={container()}>
        <p className={emptyMsg()}>
          <FaIcon gray icon={faBan} />
          {t("history.noData")}
        </p>
      </div>
    );

  return (
    <div
      className={container()}
      style={{
        gridTemplateColumns: `repeat(${allValues.length + 1}, 1fr)`,
      }}
    >
      <div />
      {allValues.map((val) => (
        <WithTooltip key={val.label} text={val.description}>
          <ConceptBubble>{val.label}</ConceptBubble>
        </WithTooltip>
      ))}
      {years.map((year, i) => (
        <Fragment key={year}>
          <div className="text-sm">{year}</div>
          {allValues.map((val) => (
            <div
              key={val.label}
              className={bubble({ filled: valuesPerYear[i].includes(val) })}
            />
          ))}
        </Fragment>
      ))}
    </div>
  );
};
