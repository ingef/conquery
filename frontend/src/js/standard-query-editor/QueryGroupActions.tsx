import { faCalendar } from "@fortawesome/free-regular-svg-icons";
import { faBan, faTimes } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import IconButton from "../button/IconButton";
import WithTooltip from "../ui-components/WithTooltip";

// h-[18px]: to provide enough space when only the right side is rendered
const actions = tv({
  base: ["mb-[6px]", "h-[18px]", "text-left"],
});

const dateButton = tv({
  base: ["mr-[5px]", "px-[3px] py-0"],
  variants: {
    active: {
      true: "underline",
      false: "no-underline",
    },
  },
});

const excludeButton = tv({
  base: ["mr-[5px]", "px-[3px] py-0", "hover:opacity-70"],
  variants: {
    active: {
      // beats IconButton's own colors (incl. its red flag) via merge
      true: ["text-red hover:text-red", "[&_svg]:text-red"],
      false: ["text-gray-800 hover:text-gray-800", "[&_svg]:text-gray-800"],
    },
  },
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
      <div>
        <WithTooltip text={t("help.queryEditorExclude")} lazy>
          <IconButton
            className={excludeButton({ active: excludeActive })}
            red
            tight
            active={excludeActive}
            icon={faBan}
            onClick={onExcludeClick}
          >
            {t("queryEditor.exclude")}
          </IconButton>
        </WithTooltip>
        <WithTooltip text={t("help.queryEditorDate")} lazy>
          <IconButton
            className={dateButton({ active: dateActive })}
            active={dateActive}
            tight
            icon={faCalendar}
            onClick={onDateClick}
          >
            {t("queryEditor.date")}
          </IconButton>
        </WithTooltip>
      </div>
      <div className="absolute top-[5px] right-[7px]">
        <WithTooltip text={t("queryEditor.removeColumn")}>
          <IconButton tiny icon={faTimes} onClick={onDeleteGroup} />
        </WithTooltip>
      </div>
    </div>
  );
};

export default memo(QueryGroupActions);
