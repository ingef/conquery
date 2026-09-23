import { Fragment } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { exists } from "../common/helpers/exists";
import type { DragItemConceptTreeNode } from "../standard-query-editor/types";
import { C3, H5 } from "../ui-components/Typography";

const description = tv({
  base: ["flex items-center flex-wrap", "gap-x-[5px] gap-y-0"],
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
        <div className={description()}>
          <C3 as="span">{node.description}</C3>
        </div>
      )}
      {showAppendix && (
        <div className="mt-2 flex flex-col gap-[6px]">
          {selectedSelects.length > 0 && (
            <div>
              <H5 as="h4">{t("editorV2.outputSection")}</H5>
              <div className={description()}>
                <Value value={selectedSelects} />
              </div>
            </div>
          )}
          {filtersWithValues.length > 0 && (
            <div>
              <H5 as="h4">{t("editorV2.filtersSection")}</H5>
              {filtersWithValues.map((f) => (
                <div key={f.label} className={description()}>
                  <C3 as="span">{f.label}:</C3>
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
      <C3 as="span">
        {value}
        {isElement && ","}
      </C3>
    );
  } else if (typeof value === "boolean") {
    return <C3 as="span">{value ? "✔" : "✗"}</C3>;
  } else if (Array.isArray(value)) {
    return (
      <>
        {value.slice(0, 10).map((v, idx) => (
          <>
            <Value key={idx} value={v} isElement={idx < value.length - 1} />
          </>
        ))}
        {value.length > 10 && <C3 as="span">{`... +${value.length - 10}`}</C3>}
      </>
    );
  } else if (
    value instanceof Object &&
    "label" in value &&
    typeof value.label === "string"
  ) {
    return (
      <C3 as="span">
        {value.label}
        {isElement && ","}
      </C3>
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
    return <C3 as="span"></C3>;
  } else {
    return <C3 as="span">{JSON.stringify(value)}</C3>;
  }
};
