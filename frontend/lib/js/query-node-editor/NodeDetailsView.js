// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import { getConceptById } from "../category-trees/globalTreeStoreHelper";

import type { PropsType } from "./QueryNodeEditor";

import ConceptEntry from "./ConceptEntry";
import ConceptDropzone from "./ConceptDropzone";
import ContentCell from "./ContentCell";

import InputMultiSelect from "../form-components/InputMultiSelect";
import InputCheckbox from "../form-components/InputCheckbox";

const Row = styled("div")`
  margin-bottom: 10px;
`;

const RowHeading = styled("h5")`
  margin: 0 0 10px 0;
  font-size: ${({ theme }) => theme.font.sm};
`;

const NodeDetailsView = (props: PropsType) => {
  const {
    node,
    onSelectSelects,
    isExcludeTimestampsPossible,
    onToggleTimestamps,
    onDropConcept,
    onRemoveConcept
  } = props;

  const rootConcept = node.isPreviousQuery ? getConceptById(node.tree) : null;

  return (
    <ContentCell>
      {isExcludeTimestampsPossible && (
        <Row>
          <InputCheckbox
            label={T.translate("queryNodeEditor.excludeTimestamps")}
            input={{
              value: node.excludeTimestamps,
              onChange: () => onToggleTimestamps()
            }}
          />
        </Row>
      )}
      {node.selects && (
        <Row>
          <RowHeading>{T.translate("queryNodeEditor.selects")}</RowHeading>
          <InputMultiSelect
            input={{
              onChange: onSelectSelects,
              value: node.selects
                .filter(({ selected }) => !!selected)
                .map(({ id, label }) => ({ value: id, label: label }))
            }}
            options={node.selects.map(select => ({
              value: select.id,
              label: select.label
            }))}
          />
        </Row>
      )}
      {!node.isPreviousQuery && rootConcept && (
        <Row>
          <RowHeading>{rootConcept.label}</RowHeading>
          <div>
            <ConceptDropzone node={node} onDropConcept={onDropConcept} />
          </div>
          <div>
            {node.ids.map(conceptId => (
              <ConceptEntry
                key={conceptId}
                node={rootConcept}
                canRemoveConcepts={node.ids.length > 1}
                onRemoveConcept={onRemoveConcept}
                conceptId={conceptId}
              />
            ))}
          </div>
        </Row>
      )}
    </ContentCell>
  );
};

export default NodeDetailsView;
