import {
  faEuroSign,
  faFingerprint,
  faFolder,
  faInfo,
} from "@fortawesome/free-solid-svg-icons";
import { memo, useMemo, useState } from "react";
import { ToggleButtonGroup } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { Icon } from "../ui-components/Icon";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";
import { SidebarToggle } from "./SidebarControl";

export type ContentType =
  | "groupId"
  | "secondaryId"
  | "money"
  | "concept"
  | "rest"
  | "dates";
export type ContentFilterValue = Record<ContentType, boolean>;

interface Props {
  value: ContentFilterValue;
  onChange: (value: ContentFilterValue) => void;
}

const ContentControl = ({ value, onChange }: Props) => {
  const { t } = useTranslation();

  const options = useMemo(
    () => [
      {
        key: "money" as const,
        icon: faEuroSign,
        tooltip: t("history.content.money"),
      },
      {
        key: "concept" as const,
        icon: faFolder,
        tooltip: t("history.content.concept"),
      },
      {
        key: "rest" as const,
        icon: faInfo,
        tooltip: t("history.content.rest"),
      },
      {
        key: "groupId" as const,
        icon: faFingerprint,
        tooltip: t("history.content.fingerprint"),
      },
    ],
    [t],
  );

  return (
    <ToggleButtonGroup
      className="flex flex-col items-center"
      orientation="vertical"
      selectionMode="multiple"
      selectedKeys={options.filter((o) => value[o.key]).map((o) => o.key)}
      onSelectionChange={(keys) =>
        onChange({
          ...value,
          ...Object.fromEntries(options.map((o) => [o.key, keys.has(o.key)])),
        })
      }
    >
      {options.map((option) => (
        <TooltipTrigger key={option.key}>
          <SidebarToggle id={option.key} aria-label={option.tooltip}>
            <Icon
              icon={option.icon}
              className={!value[option.key] ? "text-gray-500" : undefined}
            />
          </SidebarToggle>
          <Tooltip placement="right">{option.tooltip}</Tooltip>
        </TooltipTrigger>
      ))}
    </ToggleButtonGroup>
  );
};

export const useContentControl = () => {
  const [contentFilter, setContentFilter] = useState<ContentFilterValue>({
    groupId: true,
    secondaryId: true,
    concept: true,
    money: true,
    rest: true,
    dates: true,
  });

  return {
    contentFilter,
    setContentFilter,
  };
};

export default memo(ContentControl);
