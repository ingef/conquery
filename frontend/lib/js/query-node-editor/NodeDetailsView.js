// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import { EditableText } from "../form-components";

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
    editorState,
    onSelectSelects,
    onUpdateLabel,
    isExcludeTimestampsPossible,
    onToggleTimestamps,
    onDropConcept,
    onRemoveConcept
  } = props;

  return (
    <ContentCell
      headline={
        <>
          {!node.isPreviousQuery && (
            <EditableText
              loading={false}
              text={node.label}
              selectTextOnMount={true}
              editing={editorState.editingLabel}
              onSubmit={value => {
                onUpdateLabel(value);
                editorState.onToggleEditLabel();
              }}
              onToggleEdit={editorState.onToggleEditLabel}
            />
          )}
          {node.isPreviousQuery && (node.label || node.id || node.ids)}
        </>
      }
    >
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
      {!node.isPreviousQuery && (
        <Row>
          <RowHeading>{[getConceptById(node.tree).label]}</RowHeading>
          <div>
            <ConceptDropzone node={node} onDropConcept={onDropConcept} />
          </div>
          <div>
            {node.ids.map(conceptId => (
              <ConceptEntry
                key={conceptId}
                node={getConceptById(conceptId)}
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
