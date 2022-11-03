import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import type { ConceptIdT } from "../api/types";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import { Heading3 } from "../headings/Headings";
import { nodeIsConceptQueryNode, NodeResetConfig } from "../model/node";
import type {
  DragItemConceptTreeNode,
  StandardQueryNodeT,
} from "../standard-query-editor/types";

import AdditionalConceptNodeChildren from "./AdditionalConceptNodeChildren";
import { HeadingBetween } from "./HeadingBetween";
import MenuColumnItem from "./MenuColumnItem";

const FixedColumn = styled("div")<{ isEmpty?: boolean }>`
  display: flex;
  flex-direction: column;
  width: ${({ isEmpty }) => (isEmpty ? "200px" : "270px")};
  overflow: hidden;
  flex-shrink: 0;
  flex-grow: 1;
  height: 100%;

  &:first-of-type {
    border-right: 1px solid ${({ theme }) => theme.col.grayLight};
  }
`;

const DimmedNote = styled(Heading3)`
  color: ${({ theme }) => theme.col.grayLight};
  padding: 15px;
  font-weight: 400;
`;

const CommonSettingsLabel = styled(Heading3)`
  padding: 15px 15px 0;
  margin: 0;
  cursor: pointer;
  &:hover {
    text-decoration: underline;
  }
`;

interface PropsT {
  className?: string;

  node: StandardQueryNodeT;
  selectedTableIdx: number | null;
  showTables: boolean;
  allowlistedTables?: string[];
  blocklistedTables?: string[];

  onCommonSettingsClick: () => void;
  onDropConcept: (node: DragItemConceptTreeNode) => void;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
  onToggleTable: (tableIdx: number, isExcluded: boolean) => void;
  onSelectTable: (tableIdx: number) => void;
  onResetTable: (tableIdx: number, config: NodeResetConfig) => void;
}

const MenuColumn: FC<PropsT> = ({
  className,
  node,
  selectedTableIdx,
  showTables,
  blocklistedTables,
  allowlistedTables,
  onCommonSettingsClick,
  onDropConcept,
  onRemoveConcept,
  onToggleTable,
  onSelectTable,
  onResetTable,
}) => {
  const { t } = useTranslation();
  const isOnlyOneTableIncluded =
    nodeIsConceptQueryNode(node) &&
    node.tables.filter((table) => !table.exclude).length === 1;

  const rootConcept = nodeIsConceptQueryNode(node)
    ? getConceptById(node.tree)
    : null;

  const isEmpty =
    !nodeIsConceptQueryNode(node) ||
    (!showTables &&
      (!rootConcept ||
        !rootConcept.children ||
        rootConcept.children.length === 0));

  return (
    <FixedColumn className={className} isEmpty={isEmpty}>
      {isEmpty && (
        <DimmedNote>{t("queryNodeEditor.emptyMenuColumn")}</DimmedNote>
      )}
      {nodeIsConceptQueryNode(node) && showTables && (
        <>
          <CommonSettingsLabel onClick={onCommonSettingsClick}>
            {t("queryNodeEditor.properties")}
          </CommonSettingsLabel>
          <HeadingBetween>
            {t("queryNodeEditor.conceptNodeTables")}
          </HeadingBetween>
          {node.tables.map((table, tableIdx) => (
            <MenuColumnItem
              key={tableIdx}
              table={table}
              isActive={selectedTableIdx === tableIdx}
              isOnlyOneTableIncluded={isOnlyOneTableIncluded}
              blocklistedTables={blocklistedTables}
              allowlistedTables={allowlistedTables}
              onClick={() => {
                if (!table.exclude) {
                  onSelectTable(tableIdx);
                }
              }}
              onToggleTable={(value) => onToggleTable(tableIdx, value)}
              onResetTable={(config: NodeResetConfig) =>
                onResetTable(tableIdx, config)
              }
            />
          ))}
        </>
      )}
      {nodeIsConceptQueryNode(node) &&
        rootConcept &&
        rootConcept.children &&
        rootConcept.children.length > 0 && (
          <AdditionalConceptNodeChildren
            node={node}
            rootConcept={rootConcept}
            onDropConcept={onDropConcept}
            onRemoveConcept={onRemoveConcept}
          />
        )}
    </FixedColumn>
  );
};

export default MenuColumn;
