import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import styled from "@emotion/styled";

import MenuColumnItem from "./MenuColumnItem";

import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import ConceptDropzone from "./ConceptDropzone";
import ConceptEntry from "./ConceptEntry";
import { QueryNodeEditorStateT } from "./reducer";
import type {
  DraggedNodeType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import { ConceptIdT } from "js/api/types";
import { Heading5 } from "js/headings/Headings";

const FixedColumn = styled("div")`
  display: flex;
  flex-direction: column;
  width: 270px;
  overflow: hidden;
  flex-shrink: 0;
  flex-grow: 1;

  &:first-of-type {
    border-right: 1px solid ${({ theme }) => theme.col.grayLight};
  }
`;

const Padded = styled("div")`
  padding: 0 15px;
`;

const HeadingBetween = styled(Heading5)`
  margin: 15px 15px 0;
`;
const Heading5Highlighted = styled(Heading5)`
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
  margin: 10px 0 5px;
`;

interface PropsT {
  className?: string;

  node: StandardQueryNodeT;
  editorState: QueryNodeEditorStateT;
  showTables: boolean;
  allowlistedTables?: string[];
  blocklistedTables?: string[];

  onDropConcept: (node: DraggedNodeType) => void;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
  onToggleTable: (tableIdx: number, isExcluded: boolean) => void;
}

const MenuColumn: FC<PropsT> = ({
  className,
  node,
  editorState,
  showTables,
  blocklistedTables,
  allowlistedTables,
  onDropConcept,
  onRemoveConcept,
  onToggleTable,
}) => {
  const { t } = useTranslation();
  const isOnlyOneTableIncluded =
    !node.isPreviousQuery &&
    node.tables.filter((table) => !table.exclude).length === 1;

  const rootConcept = !node.isPreviousQuery ? getConceptById(node.tree) : null;

  return (
    <FixedColumn className={className}>
      {!node.isPreviousQuery && showTables && (
        <div>
          <HeadingBetween>
            {t("queryNodeEditor.conceptNodeTables")}
          </HeadingBetween>
          {node.tables.map((table, tableIdx) => (
            <MenuColumnItem
              key={tableIdx}
              table={table}
              isActive={
                editorState.selectedInputTableIdx === tableIdx &&
                !editorState.detailsViewActive
              }
              isOnlyOneTableIncluded={isOnlyOneTableIncluded}
              blocklistedTables={blocklistedTables}
              allowlistedTables={allowlistedTables}
              onClick={() => editorState.onSelectInputTableView(tableIdx)}
              onToggleTable={(value) => onToggleTable(tableIdx, value)}
            />
          ))}
        </div>
      )}
      {!node.isPreviousQuery && rootConcept && rootConcept.children && (
        <>
          <HeadingBetween>
            {t("queryNodeEditor.dropMoreConcepts")}
          </HeadingBetween>
          <Padded>
            <Heading5Highlighted>{rootConcept.label}</Heading5Highlighted>
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
          </Padded>
        </>
      )}
    </FixedColumn>
  );
};

export default MenuColumn;
