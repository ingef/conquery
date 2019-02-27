// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import FaIcon from "../icon/FaIcon";

import { EditableText } from "../form-components";
import TransparentHeaderButton from "../button/TransparentHeaderButton";

import { getConceptById } from "../category-trees/globalTreeStoreHelper";

import type { PropsType } from "./QueryNodeEditor";

import ConceptEntry from "./ConceptEntry";
import ConceptDropzone from "./ConceptDropzone";
import ColumnWithContent from "./ColumnWithContent";

const Row = styled("div")`
  margin-bottom: 10px;
`;

const RowHeading = styled("h5")`
  margin: 0 0 10px 0;
  font-size: ${({ theme }) => theme.font.sm};
`;

export const NodeDetailsView = (props: PropsType) => {
  const { node, editorState } = props;

  return (
    <ColumnWithContent
      headline={
        <>
          {!node.isPreviousQuery && (
            <EditableText
              loading={false}
              text={node.label}
              selectTextOnMount={true}
              editing={editorState.editingLabel}
              onSubmit={value => {
                props.onUpdateLabel(value);
                editorState.onToggleEditLabel();
              }}
              onToggleEdit={editorState.onToggleEditLabel}
            />
          )}
          {node.isPreviousQuery && (node.label || node.id || node.ids)}
        </>
      }
    >
      {props.isExcludeTimestampsPossible && (
        <Row>
          <TransparentHeaderButton
            onClick={() => props.onToggleTimestamps(!node.excludeTimestamps)}
          >
            <FaIcon
              icon={node.excludeTimestamps ? "check-square-o" : "square-o"}
            />{" "}
            {T.translate("queryNodeEditor.excludeTimestamps")}
          </TransparentHeaderButton>
        </Row>
      )}
      {!node.isPreviousQuery && (
        <Row>
          <RowHeading>{[getConceptById(node.tree).label]}</RowHeading>
          <div>
            <ConceptDropzone node={node} onDropConcept={props.onDropConcept} />
          </div>
          <div>
            {node.ids.map(conceptId => (
              <ConceptEntry
                key={conceptId}
                node={getConceptById(conceptId)}
                canRemoveConcepts={node.ids.length > 1}
                onRemoveConcept={props.onRemoveConcept}
                conceptId={conceptId}
              />
            ))}
          </div>
        </Row>
      )}
    </ColumnWithContent>
  );
};
