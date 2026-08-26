import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import type { SelectOptionT } from "../api/types";
import { downloadBlob } from "../common/helpers/downloadBlob";
import { toCSV } from "../file/csv";
import { setMessage } from "../snack-message/actions";
import type { EntityIdsStatus } from "./History";
import type { LoadingPayload } from "./LoadHistoryDropzone";
import type { EntityId } from "./reducer";

export const saveHistory = ({
  entityIds,
  entityIdsStatus,
}: {
  entityIds: EntityId[];
  entityIdsStatus: EntityIdsStatus;
}) => {
  const usedStatuses = Object.values(entityIdsStatus).reduce(
    (longest, el) => (longest.length > el.length ? longest : el),
    [],
  );

  // Store data as CSV in format:
  // kind;id;status1;status2;...
  const idToRow = (entityId: EntityId) => [
    entityId.kind, // First column
    entityId.id, // Second column
    ...usedStatuses // Rest of the columns
      .map((opt) =>
        entityIdsStatus[entityId.id].find((s) => s.value === opt.value)
          ? (opt.value as string)
          : "",
      ),
  ];

  const csvString = toCSV(entityIds.map(idToRow));

  const blob = new Blob([csvString], {
    type: "application/csv",
  });

  downloadBlob(blob, "list.csv");
};

export const useLoadHistory = ({
  onLoadFromFile,
}: {
  onLoadFromFile: (payload: LoadingPayload) => void;
}) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();

  return useCallback(
    ({ label, data }: { label: string; data: string[][] }) => {
      const distinctEntityIds: Set<string> = new Set();
      const loadedEntityIds: EntityId[] = [];
      const loadedEntityStatus: EntityIdsStatus = {};

      // Expect data to be a CSV in format:
      // kind;id;status1;status2;...
      for (const row of data) {
        if (row.length < 2) {
          continue;
        }

        const [kind, id] = row;

        // Deduplication is necessary for SecondaryId Queries
        if (distinctEntityIds.has(kind + id)) {
          continue;
        }

        distinctEntityIds.add(kind + id);
        loadedEntityIds.push({ kind: kind, id: id });

        if (row.length > 2) {
          loadedEntityStatus[id] = row
            .slice(2)
            .filter((str) => str.length > 0)
            .map((s) => {
              const opt = s.trim();
              return { label: opt, value: opt };
            });
        }
      }

      const loadedEntityStatusOptions: SelectOptionT[] = [
        ...new Set(Object.values(loadedEntityStatus).flatMap((val) => val)),
      ];

      if (distinctEntityIds.size === 0) {
        dispatch(
          setMessage({
            message: t("history.load.error"),
            type: "error",
          }),
        );
        return;
      }

      onLoadFromFile({
        label,
        loadedEntityIds,
        loadedEntityStatus,
        loadedEntityStatusOptions,
      });
    },
    [dispatch, t, onLoadFromFile],
  );
};
