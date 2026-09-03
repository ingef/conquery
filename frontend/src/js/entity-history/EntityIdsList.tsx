import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useMemo } from "react";
import { tv } from "tailwind-variants";

import { IncrementalList } from "../common/components/IncrementalList";
import { Icon } from "../ui-components/Icon";
import type { useUpdateHistorySession } from "./actions";
import type { EntityIdsStatus } from "./History";
import type { EntityId } from "./reducer";

const row = tv({
  base: [
    "flex items-center",
    "gap-[3px]",
    "h-6",
    "px-[3px] py-px",
    "cursor-pointer",
    "text-xs",
  ],
  variants: {
    active: {
      true: "bg-primary-50",
      false: "bg-white hover:bg-gray-50",
    },
  },
});

const entityStatus = tv({
  base: [
    "rounded",
    "border-2 border-primary-500",
    "bg-white",
    "px-1",
    "text-xs",
    "text-primary-500",
    "font-bold",
  ],
});

const blurrable = tv({
  variants: {
    blurred: { true: "blur-[6px]" },
  },
});

export const EntityIdsList = ({
  blurred,
  currentEntityId,
  entityIds,
  entityIdsStatus,
  updateHistorySession,
  loadingId,
}: {
  blurred?: boolean;
  currentEntityId: EntityId | null;
  entityIds: EntityId[];
  updateHistorySession: ReturnType<
    typeof useUpdateHistorySession
  >["updateHistorySession"];
  entityIdsStatus: EntityIdsStatus;
  loadingId?: string;
}) => {
  const numberWidth = useMemo(() => {
    const magnitude = Math.ceil(Math.log(entityIds.length) / Math.log(10));

    return 15 + 6 * magnitude;
  }, [entityIds.length]);

  const renderItem = (index: number) => {
    const entityId = entityIds[index];

    return (
      // biome-ignore lint/a11y/useKeyWithClickEvents: TODO make this a real button row
      // biome-ignore lint/a11y/noStaticElementInteractions: see above
      <div
        key={entityId.id}
        className={row({
          active: entityId.id === currentEntityId?.id,
          className: "scrollable-list-item",
        })}
        onClick={() => updateHistorySession({ entityId, years: [] })}
      >
        <div
          className="shrink-0 text-xs text-gray-500"
          style={{ width: numberWidth }}
        >
          #{index + 1}
        </div>
        <div className="shrink-0 font-bold">
          <span className={blurrable({ blurred })}>{entityId.id}</span>{" "}
          <span className="font-light text-gray-500">({entityId.kind})</span>
        </div>
        {loadingId === entityId.id && (
          <Icon icon={faSpinner} className="mx-[6px] my-[3px]" />
        )}
        <div className="ml-auto flex items-center gap-[2px]">
          {entityIdsStatus[entityId.id] &&
            entityIdsStatus[entityId.id].map((val) => (
              <div className={entityStatus()} key={val.value}>
                {val.label}
              </div>
            ))}
        </div>
      </div>
    );
  };

  return <IncrementalList renderItem={renderItem} length={entityIds.length} />;
};
