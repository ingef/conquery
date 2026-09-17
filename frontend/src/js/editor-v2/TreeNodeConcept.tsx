import { Fragment } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { exists } from "../common/helpers/exists";
import type { DragItemConceptTreeNode } from "../standard-query-editor/types";

const sectionHeading = tv({
  base: ["font-bold", "text-primary-500", "uppercase", "text-xs"],
});

const description = tv({
  base: [
    "flex items-center flex-wrap",
    "gap-x-[5px] gap-y-0",
    "text-xs",
    "text-gray-800",
  ],
});

export const TreeNodeConcept = ({
  node,
  featureContentInfos,
}: {
  node: DragItemConceptTreeNode;
  featureContentInfos?: boolean;
}) => {
  const { t } = useTranslation();
  const selectedSelects = [
    ...node.selects,
    ...node.tables.flatMap((t) => t.selects),
  ].filter((s) => s.selected);

  const filtersWithValues = node.tables.flatMap((t) =>
    t.filters.filter(
      (f) => exists(f.value) && (!Array.isArray(f.value) || f.value.length > 0),
    ),
  );

  const showAppendix =
    featureContentInfos &&
    (selectedSelects.length > 0 || filtersWithValues.length > 0);

  return (
    <>
      {node.description && (
        <div className={description()}>{node.description}</div>
      )}
      {showAppendix && (
        <div className="mt-2 flex flex-col gap-[6px]">
          {selectedSelects.length > 0 && (
            <div>
              <h4 className={sectionHeading()}>
                {t("editorV2.outputSection")}
              </h4>
              <div className={description()}>
                <Value value={selectedSelects} />
              </div>
            </div>
          )}
          {filtersWithValues.length > 0 && (
            <div>
              <h4 className={sectionHeading()}>
                {t("editorV2.filtersSection")}
              </h4>
              {filtersWithValues.map((f) => (
                <div key={f.label} className={description()}>
                  <span className="font-normal">{f.label}:</span>
                  <Value value={f.value} />
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </>
  );
};

const Value = ({
  value,
  isElement,
}: {
  value: unknown;
  isElement?: boolean;
}) => {
  if (typeof value === "string" || typeof value === "number") {
    return (
      <span>
        {value}
        {isElement && ","}
      </span>
    );
  } else if (typeof value === "boolean") {
    return <span>{value ? "✔" : "✗"}</span>;
  } else if (Array.isArray(value)) {
    return (
      <>
        {value.slice(0, 10).map((v, idx) => (
          <>
            <Value key={idx} value={v} isElement={idx < value.length - 1} />
          </>
        ))}
        {value.length > 10 && <span>{`... +${value.length - 10}`}</span>}
      </>
    );
  } else if (
    value instanceof Object &&
    "label" in value &&
    typeof value.label === "string"
  ) {
    return (
      <span>
        {value.label}
        {isElement && ","}
      </span>
    );
  } else if (value instanceof Object) {
    return (
      <>
        {Object.entries(value)
          .filter(([, v]) => exists(v))
          .map(([k, v]) => (
            <Fragment key={k}>
              {k}: <Value value={v} />
            </Fragment>
          ))}
      </>
    );
  } else if (value === null) {
    return <span></span>;
  } else {
    return <span>{JSON.stringify(value)}</span>;
  }
};
