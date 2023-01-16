import styled from "@emotion/styled";
import { NativeTypes } from "react-dnd-html5-backend";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import type { SelectOptionT } from "../api/types";
import { parseCSV } from "../file/csv";
import { setMessage } from "../snack-message/actions";
import DropzoneWithFileInput, {
  DragItemFile,
} from "../ui-components/DropzoneWithFileInput";

import type { EntityIdsStatus } from "./History";
import { DEFAULT_ID_KIND } from "./actions";
import { EntityId } from "./reducer";
import { useLoadHistory } from "./saveAndLoad";

const ImportButtonSpacer = styled("div")`
  height: 30px;
`;

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
          notificationType: "error",
        }),
      );
      return;
    }

    loadHistory({ label: file.name, data });
  };

  const onImportLines = (lines: string[]) => {
    const data = lines.map((line) => line.split(";"));

    loadHistory({ label: t("importModal.pasted"), data });
  };

  return (
    <DropzoneWithFileInput
      className={className}
      acceptedDropTypes={acceptedDropTypes}
      onDrop={onDrop}
      disableClick
      showImportButton
      onImportLines={onImportLines}
      importPlaceholder={t("history.load.importPlaceholder", {
        idkind: DEFAULT_ID_KIND,
      })}
      importDescription={t("history.load.importDescription")}
    >
      {() => (
        <>
          <ImportButtonSpacer />
          {children}
        </>
      )}
    </DropzoneWithFileInput>
  );
};
