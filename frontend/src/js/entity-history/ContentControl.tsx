import {
  faEuroSign,
  faFingerprint,
  faFolder,
  faInfo,
} from "@fortawesome/free-solid-svg-icons";
import { memo, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";

import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

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
    <div className="flex flex-col items-center">
      {options.map((option) => {
        const active = value[option.key];
        return (
          <TooltipTrigger key={option.key}>
            <Button
              aria-label={option.tooltip}
              intent="tertiary"
              aria-pressed={active}
              onPress={() => {
                onChange({ ...value, [option.key]: !value[option.key] });
              }}
            >
              <Icon
                icon={option.icon}
                className={!active ? "text-gray-500" : undefined}
              />
            </Button>
            <Tooltip placement="right">{option.tooltip}</Tooltip>
          </TooltipTrigger>
        );
      })}
    </div>
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
