import { useCallback, useMemo, useState } from "react";
import type { SelectOptionT } from "../api/types";
import { EntityIdsStatus } from "./History";
import { useDefaultStatusOptions } from "./useDefaultStatusOptions";

export const useEntityStatus = ({
  currentEntityId,
}: {
  currentEntityId: string | null;
}) => {
  const defaultStatusOptions = useDefaultStatusOptions();
  const [entityStatusOptions, setEntityStatusOptions] =
    useState<SelectOptionT[]>(defaultStatusOptions);

  const [entityIdsStatus, setEntityIdsStatus] = useState<EntityIdsStatus>({});
  const setCurrentEntityStatus = useCallback(
    (value: SelectOptionT[]) => {
      if (!currentEntityId) return;

      setEntityIdsStatus((curr) => ({
        ...curr,
        [currentEntityId]: value,
      }));
    },
    [currentEntityId],
  );
  const currentEntityStatus = useMemo(
    () => (currentEntityId ? entityIdsStatus[currentEntityId] || [] : []),
    [currentEntityId, entityIdsStatus],
  );

  return {
    entityStatusOptions,
    setEntityStatusOptions,
    entityIdsStatus,
    setEntityIdsStatus,
    currentEntityStatus,
    setCurrentEntityStatus,
  };
};
