import React, { FC } from "react";
import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import { sortSelects } from "../model/select";

import InputMultiSelect from "../form-components/InputMultiSelect";
import InputCheckbox from "../form-components/InputCheckbox";

import type { QueryNodeEditorPropsT } from "./QueryNodeEditor";

import ConceptEntry from "./ConceptEntry";
import ConceptDropzone from "./ConceptDropzone";
import ContentCell from "./ContentCell";

const Row = styled("div")`
  max-width: 300px;
  margin-bottom: 10px;
`;

const RowHeading = styled("h5")`
  margin: 0 0 10px 0;
  font-size: ${({ theme }) => theme.font.sm};
`;

const NodeDetailsView: FC<QueryNodeEditorPropsT> = ({
  node,
  onSelectSelects,
  isExcludeTimestampsPossible,
  isExcludeFromSecondaryIdQueryPossible,
  onToggleTimestamps,
  onToggleSecondaryIdExclude,
  onDropConcept,
  onRemoveConcept,
}) => {
  const { t } = useTranslation();
  const rootConcept = !node.isPreviousQuery ? getConceptById(node.tree) : null;

  return (
    <ContentCell>
      {isExcludeTimestampsPossible && (
        <Row>
          <InputCheckbox
            label={t("queryNodeEditor.excludeTimestamps")}
            input={{
              value: node.excludeTimestamps,
              onChange: () => onToggleTimestamps(),
            }}
          />
        </Row>
      )}
      {isExcludeFromSecondaryIdQueryPossible && (
        <Row>
          <InputCheckbox
            label={t("queryNodeEditor.excludeFromSecondaryIdQuery")}
            input={{
              value: node.excludeFromSecondaryIdQuery,
              onChange: () => onToggleSecondaryIdExclude(),
            }}
          />
        </Row>
      )}
      {node.selects && (
        <Row>
          <RowHeading>{t("queryNodeEditor.selects")}</RowHeading>
          <InputMultiSelect
            input={{
              onChange: onSelectSelects,
              value: node.selects
                .filter(({ selected }) => !!selected)
                .map(({ id, label }) => ({ value: id, label: label })),
            }}
            options={sortSelects(node.selects).map((select) => ({
              value: select.id,
              label: select.label,
            }))}
          />
        </Row>
      )}
      {!node.isPreviousQuery && rootConcept && rootConcept.children && (
        <Row>
          <RowHeading>{rootConcept.label}</RowHeading>
          <div>
            <ConceptDropzone node={node} onDropConcept={onDropConcept} />
          </div>
          <div>
            {node.ids.map((conceptId) => (
              <ConceptEntry
                key={conceptId}
                node={getConceptById(conceptId)}
                conceptId={conceptId}
                canRemoveConcepts={node.ids.length > 1}
                onRemoveConcept={onRemoveConcept}
              />
            ))}
          </div>
        </Row>
      )}
    </ContentCell>
  );
};

export default NodeDetailsView;
