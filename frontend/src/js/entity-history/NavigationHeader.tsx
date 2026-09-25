import { SlidersHorizontalIcon } from "lucide-react";
import { type Dispatch, memo, type SetStateAction } from "react";
import { DialogTrigger } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";
import { Button } from "../ui-components/Button";
import ProgressBar from "../ui-components/ProgressBar";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";
import { C1, C2, C3, H3 } from "../ui-components/Typography";

import { SettingsModal } from "./SettingsModal";

const root = tv({
  base: [
    "grid",
    "gap-2",
    "bg-white",
    "shadow-[1px_1px_5px_0px_rgba(0,0,0,0.2)]",
    "p-[14px]",
    "rounded",
  ],
});

const baseInfo = tv({
  base: ["flex justify-between", "gap-[15px]", "overflow-hidden"],
});

interface Props {
  className?: string;
  idsCount: number;
  markedCount: number;
  entityStatusOptions: SelectOptionT[];
  setEntityStatusOptions: Dispatch<SetStateAction<SelectOptionT[]>>;
}
export const NavigationHeader = memo(
  ({
    className,
    idsCount,
    markedCount,
    setEntityStatusOptions,
    entityStatusOptions,
  }: Props) => {
    const { t } = useTranslation();
    const label = useSelector<StateT, string>(
      (state) => state.entityHistory.label,
    );

    return (
      <div className={root({ className })}>
        <div className={baseInfo()}>
          <div className="min-w-0">
            <H3 truncate title={label}>
              {label}
            </H3>
            <C3 tone="muted">{t("history.history")}</C3>
          </div>
          <TooltipTrigger>
            <DialogTrigger>
              <Button
                aria-label={t("history.settings.headline")}
                intent="tertiary"
              >
                <SlidersHorizontalIcon />
              </Button>
              <SettingsModal
                setEntityStatusOptions={setEntityStatusOptions}
                entityStatusOptions={entityStatusOptions}
              />
            </DialogTrigger>
            <Tooltip>{t("history.settings.headline")}</Tooltip>
          </TooltipTrigger>
        </div>
        <div className="grid grid-cols-[auto_1fr] items-baseline gap-x-2 text-right">
          <C1 strong>{idsCount}</C1>
          <C2 tone="muted">{t("common.entitiesFound", { count: idsCount })}</C2>
          <C1 strong>{markedCount}</C1>
          <C2 tone="muted">{t("history.marked", { count: markedCount })}</C2>
        </div>
        <ProgressBar donePercent={100 * (markedCount / idsCount)} />
      </div>
    );
  },
);
