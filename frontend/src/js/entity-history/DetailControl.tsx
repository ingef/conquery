import type { IconProp } from "@fortawesome/fontawesome-svg-core";
import {
  faBullseye,
  faCircle,
  faCircleDot,
} from "@fortawesome/free-solid-svg-icons";
import { type Dispatch, memo, type SetStateAction, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";

import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

const root = tv({ base: "flex flex-col items-center" });
export type DetailLevel = "summary" | "detail" | "full";

interface Props {
  className?: string;
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

export const DetailControl = memo(
  ({ className, detailLevel, setDetailLevel }: Props) => {
    const navOptions = useButtonConfig();
    return (
      <div className={root({ className })}>
        {navOptions.map(({ value, icon, tooltip }) => {
          const selected = value === detailLevel;

          return (
            <TooltipTrigger key={value}>
              <Button
                aria-label={tooltip}
                intent="tertiary"
                key={value}
                onPress={() => setDetailLevel(value as DetailLevel)}
                aria-pressed={selected}
              >
                <Icon icon={icon} />
              </Button>
              <Tooltip placement="right">{tooltip}</Tooltip>
            </TooltipTrigger>
          );
        })}
      </div>
    );
  },
);
