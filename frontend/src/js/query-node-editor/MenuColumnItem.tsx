import { faCheckSquare, faSquare } from "@fortawesome/free-regular-svg-icons";
import { faFilter } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { NodeResetConfig } from "../model/node";
import { tableHasFilterValues, tableIsDisabled } from "../model/table";
import type { TableWithFilterValueT } from "../standard-query-editor/types";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

const container = tv({
  base: [
    "flex flex-row items-center justify-between",
    "w-full",
    "bg-transparent",
    "px-[15px] py-2",
    "text-base",
    "leading-[21px]",
    "font-bold",
    "text-left",
    "cursor-pointer",
    "hover:underline",
  ],
  variants: {
    disabled: {
      true: "text-gray-500",
      false: "text-gray-800",
    },
  },
});

const MenuColumnItem = ({
  table,
  isOnlyOneTableIncluded,
  blocklistedTables,
  allowlistedTables,
  onClick,
  onToggleTable,
  onResetTable,
}: {
  table: TableWithFilterValueT;
  isActive: boolean;
  isOnlyOneTableIncluded: boolean;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  onClick: () => void;
  onToggleTable: (value: boolean) => void;
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
    // biome-ignore lint/a11y/useKeyWithClickEvents: TODO make the table row a real button
    // biome-ignore lint/a11y/noStaticElementInteractions: see above
    <div className={container({ disabled: isDisabled })} onClick={onClick}>
      <div className="flex items-center">
        <Button
          intent="tertiary"
          isDisabled={isDisabled || (!includable && !excludable)}
          onPress={() => {
            // To prevent selecting the table as well, see above

            if (isDisabled) {
              return;
            }

            if (includable || excludable) {
              onToggleTable(!table.exclude);
            }
          }}
          size="sm"
        >
          <Icon
            icon={includable ? faSquare : faCheckSquare}
            className="size-5"
          />
        </Button>
        <span className="pl-[10px] leading-[20px]">{table.label}</span>
      </div>
      {isFilterActive && (
        <TooltipTrigger>
          <Button
            aria-label={t("queryNodeEditor.clearSettings")}
            intent="tertiary"
            aria-pressed
            onPress={() => {
              // To prevent selecting the table as well, see above

              if (isDisabled) {
                return;
              }

              onResetTable({ useDefaults: false });
            }}
          >
            <Icon icon={faFilter} />
          </Button>
          <Tooltip>{t("queryNodeEditor.clearSettings")}</Tooltip>
        </TooltipTrigger>
      )}
    </div>
  );
};

export default MenuColumnItem;
