import { faSliders } from "@fortawesome/free-solid-svg-icons";
import { type Dispatch, memo, type SetStateAction, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type { SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";
import IconButton from "../button/IconButton";
import ProgressBar from "../common/components/ProgressBar";
import { Heading3 } from "../headings/Headings";
import WithTooltip from "../ui-components/WithTooltip";

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

const heading = tv({
  base: [
    "shrink-0",
    "m-0",
    "whitespace-nowrap",
    "text-ellipsis",
    "overflow-hidden",
  ],
  variants: {
    end: { true: "justify-self-end" },
  },
});

const infoText = tv({
  base: ["text-base", "text-gray-500", "font-normal"],
});

const specialText = tv({
  base: ["m-0", "text-xs", "uppercase", "font-normal", "text-gray-500"],
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

    const [settingsModalOpen, setSettingsModalOpen] = useState(false);

    return (
      <div className={root({ className })}>
        {settingsModalOpen && (
          <SettingsModal
            onClose={() => setSettingsModalOpen(false)}
            setEntityStatusOptions={setEntityStatusOptions}
            entityStatusOptions={entityStatusOptions}
          />
        )}
        <div className={baseInfo()}>
          <div style={{ overflow: "hidden" }}>
            <Heading3 className={heading()} title={label}>
              {label}
            </Heading3>
            <p className={specialText()}>{t("history.history")}</p>
          </div>
          <WithTooltip text={t("history.settings.headline")}>
            <IconButton
              icon={faSliders}
              onClick={() => setSettingsModalOpen(true)}
            />
          </WithTooltip>
        </div>
        <div className="grid gap-x-2 grid-cols-[auto_1fr] items-center">
          <Heading3 className={heading({ end: true })}>{idsCount}</Heading3>
          <span className={infoText()}>
            {t("common.entitiesFound", { count: idsCount })}
          </span>
          <Heading3 className={heading({ end: true })}>{markedCount}</Heading3>
          <span className={infoText()}>
            {t("history.marked", { count: markedCount })}
          </span>
        </div>
        <ProgressBar donePercent={100 * (markedCount / idsCount)} />
      </div>
    );
  },
);
