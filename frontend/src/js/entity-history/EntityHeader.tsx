import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type { SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";
import { C3, H3 } from "../ui-components/Typography";
import { BadgeToggleButton } from "./BadgeToggleButton";

import type { EntityId } from "./reducer";

const root = tv({
  base: [
    "flex items-center justify-between",
    "gap-[30px]",
    "w-full",
    "pl-[10px]",
  ],
});

const buttons = tv({
  base: ["grid grid-rows-[1fr_1fr] grid-flow-col", "gap-[5px]"],
});

const entityId = tv({
  variants: {
    blurred: { true: "blur-[6px]" },
  },
});

export const EntityHeader = ({
  blurred,
  className,
  currentEntityIndex,
  currentEntityId,
  status,
  setStatus,
  entityStatusOptions,
}: {
  blurred?: boolean;
  className?: string;
  currentEntityIndex: number;
  currentEntityId: EntityId;
  status: SelectOptionT[];
  setStatus: (value: SelectOptionT[]) => void;
  entityStatusOptions: SelectOptionT[];
}) => {
  const totalEvents = useSelector<StateT, number>(
    (state) => state.entityHistory.currentEntityData.length,
  );

  const { t } = useTranslation();

  const toggleOption = (option: SelectOptionT) => () => {
    const newStatus = [...status];
    const index = newStatus.findIndex((val) => val.value === option.value);
    if (index === -1) {
      newStatus.push(option);
    } else {
      newStatus.splice(index, 1);
    }
    setStatus(newStatus);
  };

  return (
    <div className={root({ className })}>
      <div className="flex items-center gap-[30px]">
        <div>
          <div className="flex gap-[5px]">
            <H3 as="span" tone="muted">
              #{currentEntityIndex + 1}
            </H3>
            <span className={entityId({ blurred })}>
              <H3 as="span">{currentEntityId.id}</H3>
            </span>
          </div>
          <div className="mt-[5px]">
            <C3 tone="muted">
              {totalEvents} {t("history.events", { count: totalEvents })}
            </C3>
          </div>
        </div>
      </div>
      <div className={buttons()}>
        {entityStatusOptions.map((option, i) => (
          <span key={option.label + i}>
            <BadgeToggleButton
              active={!!status.find((opt) => opt.value === option.value)}
              onClick={toggleOption(option)}
              hotkey={i < 9 ? String(i + 1) : undefined}
            >
              {option.label}
            </BadgeToggleButton>
          </span>
        ))}
      </div>
    </div>
  );
};
