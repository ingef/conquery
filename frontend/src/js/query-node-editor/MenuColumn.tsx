import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { ConceptIdT } from "../api/types";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import { type NodeResetConfig, nodeIsConceptQueryNode } from "../model/node";
import type {
  DragItemConceptTreeNode,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import { GridList, GridListItem } from "../ui-components/GridList";
import { C2, H4 } from "../ui-components/Typography";
import AdditionalConceptNodeChildren from "./AdditionalConceptNodeChildren";
import MenuColumnItem from "./MenuColumnItem";

const fixedColumn = tv({
  base: [
    "flex flex-col",
    "gap-3",
    "shrink-0 grow",
    "h-full",
    "overflow-hidden",
    "p-[10px]",
    "first-of-type:border-r first-of-type:border-r-gray-100",
  ],
  variants: {
    isEmpty: {
      true: "w-[200px]",
      false: "w-[270px]",
    },
  },
});

const COMMON_SETTINGS = "common";

/**
 * The node editor's navigation: the sections of the content column, with
 * the controls that apply to a whole source (include it, clear its filters)
 * and the concepts the node holds.
 */
const MenuColumn = ({
  className,
  node,
  selectedTableIdx,
  showTables,
  blocklistedTables,
  allowlistedTables,
  onSelectCommonSettings,
  onDropConcept,
  onRemoveConcept,
  onToggleTable,
  onSelectTable,
  onResetTable,
}: {
  className?: string;

  node: StandardQueryNodeT;
  selectedTableIdx: number | null;
  showTables: boolean;
  allowlistedTables?: string[];
  blocklistedTables?: string[];

  onSelectCommonSettings: () => void;
  onDropConcept: (node: DragItemConceptTreeNode) => void;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
  onToggleTable: (tableIdx: number, isExcluded: boolean) => void;
  onSelectTable: (tableIdx: number) => void;
  onResetTable: (tableIdx: number, config: NodeResetConfig) => void;
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
      (!rootConcept?.children || rootConcept.children.length === 0));

  return (
    <div className={fixedColumn({ isEmpty, className })}>
      {isEmpty && <C2 tone="muted">{t("queryNodeEditor.emptyMenuColumn")}</C2>}
      {nodeIsConceptQueryNode(node) && showTables && (
        <>
          <GridList
            aria-label={t("queryNodeEditor.sections")}
            selectionMode="single"
            selectedKeys={selectedTableIdx === null ? [COMMON_SETTINGS] : []}
            onSelectionChange={onSelectCommonSettings}
          >
            <GridListItem
              id={COMMON_SETTINGS}
              textValue={t("queryNodeEditor.properties")}
            >
              <C2 truncate>{t("queryNodeEditor.properties")}</C2>
            </GridListItem>
          </GridList>
          <H4>{t("queryNodeEditor.conceptNodeTables")}</H4>
          <GridList
            aria-label={t("queryNodeEditor.conceptNodeTables")}
            selectionMode="single"
            selectedKeys={
              selectedTableIdx === null ? [] : [String(selectedTableIdx)]
            }
            onSelectionChange={(keys) => {
              if (keys === "all") return;
              const tableIdx = Number([...keys][0]);
              if (!node.tables[tableIdx]?.exclude) onSelectTable(tableIdx);
            }}
          >
            {node.tables.map((table, tableIdx) => (
              <MenuColumnItem
                key={tableIdx}
                id={String(tableIdx)}
                table={table}
                isOnlyOneTableIncluded={isOnlyOneTableIncluded}
                blocklistedTables={blocklistedTables}
                allowlistedTables={allowlistedTables}
                onToggleTable={(value) => onToggleTable(tableIdx, value)}
                onResetTable={(config: NodeResetConfig) =>
                  onResetTable(tableIdx, config)
                }
              />
            ))}
          </GridList>
        </>
      )}
      {nodeIsConceptQueryNode(node) &&
        rootConcept?.children &&
        rootConcept.children.length > 0 && (
          <AdditionalConceptNodeChildren
            node={node}
            rootConcept={rootConcept}
            onDropConcept={onDropConcept}
            onRemoveConcept={onRemoveConcept}
          />
        )}
    </div>
  );
};

export default MenuColumn;
