import { faCheckCircle } from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { QueryUploadConfigT, UploadQueryResponseT } from "../../api/types";
import DropzoneWithFileInput from "../../ui-components/DropzoneWithFileInput";
import { Icon } from "../../ui-components/Icon";
import InfoTooltip from "../../ui-components/InfoTooltip";
import { Modal, ModalBody, ModalHeader } from "../../ui-components/Modal";

import CSVColumnPicker, { type QueryToUploadT } from "./CSVColumnPicker";

const successIcon = tv({
  base: ["block", "mx-auto mb-[10px]", "size-10", "text-green"],
});

const dropzone = tv({
  base: ["w-full", "cursor-pointer", "py-[180px]"],
});

const UploadQueryResultsModal = ({
  loading,
  config,
  uploadResult,
  onClearUploadResult,
  onUpload,
}: {
  loading: boolean;
  config: QueryUploadConfigT;
  uploadResult: UploadQueryResponseT | null;
  onClearUploadResult: () => void;
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
    <Modal size="lg" scrollable>
      {({ close }) => (
        <>
          <ModalHeader>
            {t("uploadQueryResultsModal.headline")}
            <InfoTooltip
              size="wide"
              text={t("uploadQueryResultsModal.formatInfo.text")}
            />
          </ModalHeader>
          <ModalBody>
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
                    onCancel={close}
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
          </ModalBody>
        </>
      )}
    </Modal>
  );
};

export default UploadQueryResultsModal;
