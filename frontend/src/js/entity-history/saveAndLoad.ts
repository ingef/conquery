import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import { downloadBlob } from "../common/helpers/downloadBlob";
import { toCSV } from "../file/csv";
import { setMessage } from "../snack-message/actions";

import { EntityIdsStatus } from "./History";
import { LoadingPayload } from "./LoadHistoryDropzone";
import { EntityId } from "./reducer";

export const saveHistory = ({
  entityIds,
  entityIdsStatus,
}: {
  entityIds: EntityId[];
  entityIdsStatus: EntityIdsStatus;
}) => {
  console.log(entityIdsStatus);
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
      const loadedEntityIds: EntityId[] = [];
      const loadedEntityStatus: EntityIdsStatus = {};
      const loadedEntityStatusOptionsRaw: string[] = [];

      // Expect data to be a CSV in format:
      // kind;id;status1;status2;...
      for (const row of data) {
        if (row.length < 2) {
          continue;
        }

        const [kind, id] = row;

        loadedEntityIds.push({ kind, id });

        if (row.length > 2) {
          loadedEntityStatus[id] = row
            .slice(2)
            .filter((str) => str.length > 0)
            .map((s) => {
              const opt = s.trim();
              loadedEntityStatusOptionsRaw.push(opt);
              return { label: opt, value: opt };
            });
        }
      }

      const loadedEntityStatusOptions = [
        ...new Set(loadedEntityStatusOptionsRaw),
      ].map((item) => ({ label: item, value: item }));

      if (loadedEntityIds.length === 0) {
        dispatch(
          setMessage({
            message: t("history.load.error"),
            notificationType: "error",
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
