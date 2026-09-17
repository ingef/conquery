import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { ConceptIdT } from "../api/types";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import { Heading3 } from "../headings/Headings";
import { type NodeResetConfig, nodeIsConceptQueryNode } from "../model/node";
import type {
  DragItemConceptTreeNode,
  StandardQueryNodeT,
} from "../standard-query-editor/types";

import AdditionalConceptNodeChildren from "./AdditionalConceptNodeChildren";
import { HeadingBetween } from "./HeadingBetween";
import MenuColumnItem from "./MenuColumnItem";

const fixedColumn = tv({
  base: [
    "flex flex-col",
    "shrink-0 grow",
    "h-full",
    "overflow-hidden",
    "first-of-type:border-r first-of-type:border-r-gray-100",
  ],
  variants: {
    isEmpty: {
      true: "w-[200px]",
      false: "w-[270px]",
    },
  },
});

const dimmedNote = tv({
  base: ["p-[15px]", "text-gray-100", "font-normal"],
});

const commonSettingsLabel = tv({
  base: [
    "px-[15px] pt-[15px] pb-0",
    "m-0",
    "cursor-pointer",
    "hover:underline",
  ],
});

const MenuColumn = ({
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
}: {
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
      {isEmpty && (
        <Heading3 className={dimmedNote()}>
          {t("queryNodeEditor.emptyMenuColumn")}
        </Heading3>
      )}
      {nodeIsConceptQueryNode(node) && showTables && (
        <>
          <Heading3
            className={commonSettingsLabel()}
            onClick={onCommonSettingsClick}
          >
            {t("queryNodeEditor.properties")}
          </Heading3>
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
