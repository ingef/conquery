import styled from "@emotion/styled";
import { Fragment } from "react";
import { useTranslation } from "react-i18next";

import { exists } from "../common/helpers/exists";
import { DragItemConceptTreeNode } from "../standard-query-editor/types";

const Bold = styled("span")`
  font-weight: 400;
`;

const SectionHeading = styled("h4")`
  font-weight: 700;
  color: ${(props) => props.theme.col.blueGrayDark};
  margin: 0;
  text-transform: uppercase;
  font-size: ${({ theme }) => theme.font.xs};
`;

const Appendix = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
`;

const Description = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.black};
  display: flex;
  align-items: center;
  gap: 0px 5px;
  flex-wrap: wrap;
`;

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
      (f) =>
        exists(f.value) && (!(f.value instanceof Array) || f.value.length > 0),
    ),
  );

  const showAppendix =
    featureContentInfos &&
    (selectedSelects.length > 0 || filtersWithValues.length > 0);

  return (
    <>
      {node.description && <Description>{node.description}</Description>}
      {showAppendix && (
        <Appendix>
          {selectedSelects.length > 0 && (
            <div>
              <SectionHeading>{t("editorV2.outputSection")}</SectionHeading>
              <Description>
                <Value value={selectedSelects} />
              </Description>
            </div>
          )}
          {filtersWithValues.length > 0 && (
            <div>
              <SectionHeading>{t("editorV2.filtersSection")}</SectionHeading>
              {filtersWithValues.map((f) => (
                <Description>
                  <Bold>{f.label}:</Bold>
                  <Value value={f.value} />
                </Description>
              ))}
            </div>
          )}
        </Appendix>
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
  } else if (value instanceof Array) {
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
