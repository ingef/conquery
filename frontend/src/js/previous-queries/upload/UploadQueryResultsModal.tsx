import styled from "@emotion/styled";
import { faCheckCircle } from "@fortawesome/free-solid-svg-icons";
import { FC, useState } from "react";
import { useTranslation } from "react-i18next";

import type { QueryUploadConfigT, UploadQueryResponseT } from "../../api/types";
import FaIcon from "../../icon/FaIcon";
import Modal from "../../modal/Modal";
import InfoTooltip from "../../tooltip/InfoTooltip";
import DropzoneWithFileInput from "../../ui-components/DropzoneWithFileInput";

import CSVColumnPicker, { QueryToUploadT } from "./CSVColumnPicker";

const Root = styled("div")``;

const Success = styled("div")`
  margin: 25px 0;
`;

const StyledFaIcon = styled(FaIcon)`
  font-size: 40px;
  display: block;
  margin: 0 auto 10px;
  color: ${({ theme }) => theme.col.green};
`;

const SuccessMsg = styled("p")`
  margin: 0;
`;

const SxDropzoneWithFileInput = styled(DropzoneWithFileInput)`
  padding: 180px 250px;
  width: 100%;
  cursor: pointer;
`;

interface PropsT {
  loading: boolean;
  config: QueryUploadConfigT;
  uploadResult: UploadQueryResponseT | null;
  onClearUploadResult: () => void;
  onClose: () => void;
  onUpload: (query: QueryToUploadT) => void;
}

const UploadQueryResultsModal: FC<PropsT> = ({
  loading,
  config,
  uploadResult,
  onClearUploadResult,
  onClose,
  onUpload,
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
      closeIcon
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
      <Root>
        {fullUploadSuccess ? (
          <Success>
            <StyledFaIcon icon={faCheckCircle} />
            <SuccessMsg>
              {t("uploadQueryResultsModal.uploadSucceeded", {
                count: uploadResult?.resolved || 0,
              })}
            </SuccessMsg>
          </Success>
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
              <SxDropzoneWithFileInput
                onDrop={(item) => {
                  if (item.type === "__NATIVE_FILE__") {
                    setFile(item.files[0]);
                  }
                }}
                onSelectFile={setFile}
                accept="text/csv"
              >
                {() => t("uploadQueryResultsModal.dropzone")}
              </SxDropzoneWithFileInput>
            )}
          </div>
        )}
      </Root>
    </Modal>
  );
};

export default UploadQueryResultsModal;
