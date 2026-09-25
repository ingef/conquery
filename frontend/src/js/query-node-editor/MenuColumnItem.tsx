import { FunnelIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import type { NodeResetConfig } from "../model/node";
import { tableHasFilterValues, tableIsDisabled } from "../model/table";
import type { TableWithFilterValueT } from "../standard-query-editor/types";
import { CheckboxField } from "../ui-components/CheckboxField";
import { GridListItem } from "../ui-components/GridList";
import { ToggleButton } from "../ui-components/ToggleButton";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";
import { C2 } from "../ui-components/Typography";

/** a source in the navigation: include it, go to its section, clear its filters */
const MenuColumnItem = ({
  id,
  table,
  isOnlyOneTableIncluded,
  blocklistedTables,
  allowlistedTables,
  onToggleTable,
  onResetTable,
}: {
  id: string;
  table: TableWithFilterValueT;
  isOnlyOneTableIncluded: boolean;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  onToggleTable: (isExcluded: boolean) => void;
  onResetTable: (config: NodeResetConfig) => void;
}) => {
  const { t } = useTranslation();
  const isDisabled = tableIsDisabled(
    table,
    blocklistedTables,
    allowlistedTables,
  );

  const includable = table.exclude;
  const excludable = !isOnlyOneTableIncluded;

  const isFilterActive = tableHasFilterValues(table);

  return (
    <GridListItem id={id} textValue={table.label}>
      <CheckboxField
        aria-label={t("queryNodeEditor.includeTable", { table: table.label })}
        isSelected={!table.exclude}
        isDisabled={isDisabled || (!includable && !excludable)}
        onChange={(include) => onToggleTable(!include)}
      />
      <div className="min-w-0 grow">
        <C2 truncate tone={isDisabled || table.exclude ? "muted" : undefined}>
          {table.label}
        </C2>
      </div>
      {/* a fixed slot, so the row keeps its layout without the button */}
      <span className="flex size-6 shrink-0 items-center justify-center">
        {isFilterActive && (
          <TooltipTrigger>
            <ToggleButton
              aria-label={t("queryNodeEditor.clearSettings")}
              intent="tertiary"
              size="sm"
              isSelected
              isDisabled={isDisabled}
              onChange={() => onResetTable({ useDefaults: false })}
            >
              <FunnelIcon />
            </ToggleButton>
            <Tooltip>{t("queryNodeEditor.clearSettings")}</Tooltip>
          </TooltipTrigger>
        )}
      </span>
    </GridListItem>
  );
};

export default MenuColumnItem;
