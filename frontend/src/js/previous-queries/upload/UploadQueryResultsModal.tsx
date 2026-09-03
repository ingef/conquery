import { faCheckCircle } from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { QueryUploadConfigT, UploadQueryResponseT } from "../../api/types";
import Modal from "../../modal/Modal";
import DropzoneWithFileInput from "../../ui-components/DropzoneWithFileInput";
import { Icon } from "../../ui-components/Icon";
import InfoTooltip from "../../ui-components/InfoTooltip";

import CSVColumnPicker, { type QueryToUploadT } from "./CSVColumnPicker";

const successIcon = tv({
  base: ["block", "mx-auto mb-[10px]", "text-[40px]", "text-green"],
});

const dropzone = tv({
  base: ["w-full", "cursor-pointer", "px-[250px] py-[180px]"],
});

const UploadQueryResultsModal = ({
  loading,
  config,
  uploadResult,
  onClearUploadResult,
  onClose,
  onUpload,
}: {
  loading: boolean;
  config: QueryUploadConfigT;
  uploadResult: UploadQueryResponseT | null;
  onClearUploadResult: () => void;
  onClose: () => void;
  onUpload: (query: QueryToUploadT) => void;
}) => {
  const { t } = useTranslation();
  const [file, setFile] = useState<File | null>(null);

  const fullUploadSuccess =
    uploadResult &&
    uploadResult.resolved > 0 &&
    uploadResult.unreadableDate.length === 0 &&
    uploadResult.unresolvedId.length === 0;

  return (
    <Modal
      onClose={onClose}
      scrollable
      headline={
        <>
          {t("uploadQueryResultsModal.headline")}
          <InfoTooltip
            wide
            text={t("uploadQueryResultsModal.formatInfo.text")}
          />
        </>
      }
    >
      <div>
        {fullUploadSuccess ? (
          <div className="my-[25px]">
            <Icon icon={faCheckCircle} className={successIcon()} />
            <p className="m-0">
              {t("uploadQueryResultsModal.uploadSucceeded", {
                count: uploadResult?.resolved || 0,
              })}
            </p>
          </div>
        ) : (
          <div>
            {file && (
              <CSVColumnPicker
                file={file}
                uploadResult={uploadResult}
                config={config}
                loading={loading}
                onUpload={onUpload}
                onCancel={onClose}
                onReset={() => {
                  setFile(null);
                  onClearUploadResult();
                }}
              />
            )}
            {!file && (
              <DropzoneWithFileInput
                className={dropzone()}
                onDrop={(item) => {
                  if (item.type === "__NATIVE_FILE__") {
                    setFile(item.files[0]);
                  }
                }}
                onSelectFile={setFile}
                accept="text/csv"
              >
                {() => t("uploadQueryResultsModal.dropzone")}
              </DropzoneWithFileInput>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
};

export default UploadQueryResultsModal;
