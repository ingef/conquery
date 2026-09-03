import { faCalendar } from "@fortawesome/free-regular-svg-icons";
import { faBan, faTimes } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { ToggleButton } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";

import {
  Tooltip,
  TooltipTrigger,
  tooltipDelay,
} from "../ui-components/Tooltip";

// h-[18px]: to provide enough space when only the right side is rendered
const actions = tv({
  base: ["mb-[6px]", "h-[18px]", "text-left"],
});

// excluding is a warning state: selected shows red, not the usual primary
const excludeToggle = tv({
  base: [
    "inline-flex items-center gap-[5px]",
    "h-6 px-2",
    "rounded border border-transparent",
    "text-xs text-gray-800",
    "cursor-pointer",
    "hover:bg-gray-50",
    "data-selected:text-red",
  ],
});

interface PropsT {
  excludeActive: boolean;
  dateActive: boolean;
  onExcludeClick: () => void;
  onDeleteGroup: () => void;
  onDateClick: () => void;
}

const QueryGroupActions = ({
  excludeActive,
  dateActive,
  onExcludeClick,
  onDeleteGroup,
  onDateClick,
}: PropsT) => {
  const { t } = useTranslation();

  return (
    <div className={actions()}>
      <div className="flex items-center gap-[5px]">
        <TooltipTrigger delay={tooltipDelay.long}>
          <ToggleButton
            className={excludeToggle()}
            isSelected={excludeActive}
            onChange={onExcludeClick}
          >
            <Icon icon={faBan} />
            {t("queryEditor.exclude")}
          </ToggleButton>
          <Tooltip>{t("help.queryEditorExclude")}</Tooltip>
        </TooltipTrigger>
        <TooltipTrigger delay={tooltipDelay.long}>
          <Button
            intent="tertiary"
            size="sm"
            aria-pressed={dateActive}
            onPress={onDateClick}
          >
            <Icon icon={faCalendar} />
            {t("queryEditor.date")}
          </Button>
          <Tooltip>{t("help.queryEditorDate")}</Tooltip>
        </TooltipTrigger>
      </div>
      <div className="absolute top-[5px] right-[7px]">
        <TooltipTrigger>
          <Button
            aria-label={t("queryEditor.removeColumn")}
            intent="tertiary"
            size="sm"
            onPress={onDeleteGroup}
          >
            <Icon icon={faTimes} />
          </Button>
          <Tooltip>{t("queryEditor.removeColumn")}</Tooltip>
        </TooltipTrigger>
      </div>
    </div>
  );
};

export default memo(QueryGroupActions);
