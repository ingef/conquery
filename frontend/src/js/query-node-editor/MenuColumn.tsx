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
import { COMMON_SECTION } from "./useSectionSpy";

const fixedColumn = tv({
  base: [
    "flex flex-col",
    "gap-3",
    "shrink-0 grow",
    "h-full",
    "overflow-hidden",
    "p-2",
    "first-of-type:border-r first-of-type:border-r-gray-100",
  ],
  variants: {
    isEmpty: {
      true: "w-[200px]",
      false: "w-[270px]",
    },
  },
});

/**
 * The node editor's navigation: the sections of the content column, with
 * the controls that apply to a whole source (include it, clear its filters)
 * and the concepts the node holds.
 */
const MenuColumn = ({
  className,
  node,
  activeSection,
  showTables,
  blocklistedTables,
  allowlistedTables,
  onDropConcept,
  onRemoveConcept,
  onToggleTable,
  onSelectSection,
  onResetTable,
}: {
  className?: string;

  node: StandardQueryNodeT;
  /** the section in view on the right, `COMMON_SECTION` or a table index */
  activeSection: string;
  showTables: boolean;
  allowlistedTables?: string[];
  blocklistedTables?: string[];

  onDropConcept: (node: DragItemConceptTreeNode) => void;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
  onToggleTable: (tableIdx: number, isExcluded: boolean) => void;
  onSelectSection: (key: string) => void;
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
            selectedKeys={
              activeSection === COMMON_SECTION ? [COMMON_SECTION] : []
            }
            onSelectionChange={() => onSelectSection(COMMON_SECTION)}
          >
            <GridListItem
              id={COMMON_SECTION}
              textValue={t("queryNodeEditor.properties")}
            >
              <C2 truncate>{t("queryNodeEditor.properties")}</C2>
            </GridListItem>
          </GridList>
          <div className="px-2">
            <H4>{t("queryNodeEditor.conceptNodeTables")}</H4>
          </div>
          <GridList
            aria-label={t("queryNodeEditor.conceptNodeTables")}
            selectionMode="single"
            selectedKeys={
              activeSection === COMMON_SECTION ? [] : [activeSection]
            }
            onSelectionChange={(keys) => {
              if (keys === "all") return;
              const key = String([...keys][0]);
              // an excluded source has no section
              if (!node.tables[Number(key)]?.exclude) onSelectSection(key);
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
          <div className="flex min-h-0 grow flex-col gap-3 px-2">
            <AdditionalConceptNodeChildren
              node={node}
              rootConcept={rootConcept}
              onDropConcept={onDropConcept}
              onRemoveConcept={onRemoveConcept}
            />
          </div>
        )}
    </div>
  );
};

export default MenuColumn;
