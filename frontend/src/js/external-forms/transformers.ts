import type {
  ConceptQueryNodeType,
  TableWithFilterValueType,
} from "../standard-query-editor/types";
import type { SelectorT } from "../api/types";
import type { ConnectorDefault as ConnectorDefaultType } from "./config-types";

import { tableIsDisabled } from "../model/table";

function setDefaultSelects(selects: SelectorT[], defaultSelects: string[]) {
  return selects.map((select) => ({
    ...select,
    selected: defaultSelects.some(
      (s) => select.id.toLowerCase().indexOf(s.toLowerCase()) !== -1
    ),
  }));
}

export const initTables = ({
  blocklistedTables,
  allowlistedTables,
}: {
  blocklistedTables?: string[];
  allowlistedTables?: string[];
}) => (node: ConceptQueryNodeType) => {
  return !node.tables
    ? node
    : {
        ...node,
        tables: node.tables.map((table) => {
          const isDisabled = tableIsDisabled(
            table,
            blocklistedTables,
            allowlistedTables
          );

          return isDisabled ? { ...table, exclude: true } : table;
        }),
      };
};

export const initTablesWithDefaults = (
  connectorDefaults?: ConnectorDefaultType[]
) => (node: ConceptQueryNodeType) => {
  return !node.tables
    ? node
    : {
        ...node,
        tables: node.tables.map((table) => {
          if (!table.selects || !connectorDefaults) return table;

          const connectorDefault = connectorDefaults.find(
            (c) => table.id.toLowerCase().indexOf(c.name.toLowerCase()) !== -1
          );

          if (!connectorDefault) return table;

          return initSelectsWithDefaults(connectorDefault.selects)(table);
        }),
      };
};

export const initSelectsWithDefaults = (defaultSelects: string[]) => (
  node: ConceptQueryNodeType | TableWithFilterValueType
) => {
  return !node.selects || !defaultSelects
    ? node
    : {
        ...node,
        selects: setDefaultSelects(node.selects, defaultSelects),
      };
};
