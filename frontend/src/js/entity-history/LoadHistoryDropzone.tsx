import { NativeTypes } from "react-dnd-html5-backend";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import type { SelectOptionT } from "../api/types";
import { parseCSV } from "../file/csv";
import { setMessage } from "../snack-message/actions";
import Dropzone from "../ui-components/Dropzone";
import { DragItemFile } from "../ui-components/DropzoneWithFileInput";

import type { EntityIdsStatus } from "./History";

const acceptedDropTypes = [NativeTypes.FILE];

export interface LoadingPayload {
  label: string;
  loadedEntityIds: string[];
  loadedEntityStatus: EntityIdsStatus;
  loadedEntityStatusOptions: SelectOptionT[];
}

interface Props {
  className?: string;
  onLoad: (payload: LoadingPayload) => void;
  children: React.ReactNode;
}

export const LoadHistoryDropzone = ({ className, onLoad, children }: Props) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const onDrop = async ({ files }: DragItemFile) => {
    const file = files[0];
    const { data } = await parseCSV(file, ";");

    console.log(data);

    if (data.length === 0) {
      dispatch(setMessage({ message: t("history.load.error") }));
      return;
    }

    const loadedEntityIds = [];
    const loadedEntityStatus: EntityIdsStatus = {};
    const loadedEntityStatusOptionsRaw: string[] = [];

    for (const row of data) {
      if (row.length !== 2) {
        continue;
      }

      loadedEntityIds.push(row[0]);
      if (row[1]) {
        loadedEntityStatus[row[0]] = row[1].split(",").map((s) => {
          const opt = s.trim();
          loadedEntityStatusOptionsRaw.push(s);
          return { label: opt, value: opt };
        });
      }
    }

    const loadedEntityStatusOptions = [
      ...new Set(loadedEntityStatusOptionsRaw),
    ].map((item) => ({ label: item, value: item }));

    if (loadedEntityIds.length === 0) {
      dispatch(setMessage({ message: t("history.load.error") }));
      return;
    }

    onLoad({
      label: file.name,
      loadedEntityIds,
      loadedEntityStatus,
      loadedEntityStatusOptions,
    });
  };

  return (
    <Dropzone<DragItemFile>
      className={className}
      acceptedDropTypes={acceptedDropTypes}
      onDrop={onDrop}
    >
      {() => children}
    </Dropzone>
  );
};
