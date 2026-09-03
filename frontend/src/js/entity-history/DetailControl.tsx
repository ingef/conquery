import type { IconProp } from "@fortawesome/fontawesome-svg-core";
import {
  faBullseye,
  faCircle,
  faCircleDot,
} from "@fortawesome/free-solid-svg-icons";
import { type Dispatch, memo, type SetStateAction, useMemo } from "react";
import type { Key } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { Icon } from "../ui-components/Icon";
import { ToggleButton } from "../ui-components/ToggleButton";
import { ToggleButtonGroup } from "../ui-components/ToggleButtonGroup";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

const detailLevels: DetailLevel[] = ["summary", "detail", "full"];
const isDetailLevel = (key: Key): key is DetailLevel =>
  detailLevels.some((level) => level === key);
export type DetailLevel = "summary" | "detail" | "full";

interface Props {
  detailLevel: DetailLevel;
  setDetailLevel: Dispatch<SetStateAction<DetailLevel>>;
}

const useButtonConfig = () => {
  const { t } = useTranslation();
  return useMemo(
    (): {
      icon: IconProp;
      value: string;
      tooltip: string;
    }[] => [
      {
        icon: faCircle,
        value: "summary",
        tooltip: t("history.detail.summary"),
      },
      {
        icon: faCircleDot,
        value: "detail",
        tooltip: t("history.detail.detail"),
      },
      {
        icon: faBullseye,
        value: "full",
        tooltip: t("history.detail.full"),
      },
    ],
    [t],
  );
};

export const DetailControl = memo(({ detailLevel, setDetailLevel }: Props) => {
  const navOptions = useButtonConfig();
  return (
    <ToggleButtonGroup
      orientation="vertical"
      selectionMode="single"
      disallowEmptySelection
      selectedKeys={[detailLevel]}
      onSelectionChange={(keys) => {
        const [key] = keys;
        if (key !== undefined && isDetailLevel(key)) setDetailLevel(key);
      }}
    >
      {navOptions.map(({ value, icon, tooltip }) => (
        <TooltipTrigger key={value}>
          <ToggleButton id={value} aria-label={tooltip}>
            <Icon icon={icon} />
          </ToggleButton>
          <Tooltip placement="right">{tooltip}</Tooltip>
        </TooltipTrigger>
      ))}
    </ToggleButtonGroup>
  );
});
