import { NativeTypes } from "react-dnd-html5-backend";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import type { SelectOptionT } from "../api/types";
import { parseCSV } from "../file/csv";
import { setMessage } from "../snack-message/actions";
import { SnackMessageType } from "../snack-message/reducer";
import DropzoneWithFileInput, {
  DragItemFile,
} from "../ui-components/DropzoneWithFileInput";

import type { EntityIdsStatus } from "./History";
import { EntityId } from "./reducer";
import { useLoadHistory } from "./saveAndLoad";

const acceptedDropTypes = [NativeTypes.FILE];

export interface LoadingPayload {
  label: string;
  loadedEntityIds: EntityId[];
  loadedEntityStatus: EntityIdsStatus;
  loadedEntityStatusOptions: SelectOptionT[];
}

interface Props {
  className?: string;
  onLoadFromFile: (payload: LoadingPayload) => void;
  children: React.ReactNode;
}

export const LoadHistoryDropzone = ({
  className,
  onLoadFromFile,
  children,
}: Props) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const loadHistory = useLoadHistory({ onLoadFromFile });

  const onDrop = async ({ files }: DragItemFile) => {
    const file = files[0];
    const { data } = await parseCSV(file, ";");

    if (data.length === 0) {
      dispatch(
        setMessage({
          message: t("history.load.error"),
          type: SnackMessageType.ERROR,
        }),
      );
      return;
    }

    loadHistory({ label: file.name, data });
  };

  return (
    <DropzoneWithFileInput
      className={className}
      acceptedDropTypes={acceptedDropTypes}
      onDrop={onDrop}
      disableClick
    >
      {() => children}
    </DropzoneWithFileInput>
  );
};
