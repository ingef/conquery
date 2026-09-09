import type { IconProp } from "@fortawesome/fontawesome-svg-core";
import {
  faBullseye,
  faCircle,
  faCircleDot,
} from "@fortawesome/free-solid-svg-icons";
import { type Dispatch, memo, type SetStateAction, useMemo } from "react";
import { useTranslation } from "react-i18next";

import { tv } from "tailwind-variants";

import IconButton from "../button/IconButton";
import WithTooltip from "../ui-components/WithTooltip";

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
            <WithTooltip key={value} text={tooltip}>
              <IconButton
                key={value}
                onClick={() => setDetailLevel(value as DetailLevel)}
                icon={icon}
                active={selected}
              />
            </WithTooltip>
          );
        })}
      </div>
    );
  },
);
