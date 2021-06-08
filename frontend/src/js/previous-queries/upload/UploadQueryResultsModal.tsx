import styled from "@emotion/styled";
import React, { FC, useState } from "react";
import { useTranslation } from "react-i18next";

import ErrorMessage from "../../error-message/ErrorMessage";
import DropzoneWithFileInput from "../../form-components/DropzoneWithFileInput";
import FaIcon from "../../icon/FaIcon";
import Modal from "../../modal/Modal";
import InfoTooltip from "../../tooltip/InfoTooltip";

import CSVColumnPicker, { ExternalQueryT } from "./CSVColumnPicker";

const Root = styled("div")`
  text-align: center;
`;

const Error = styled("div")`
  margin: 20px 0;
`;

const ErrorMessageSub = styled(ErrorMessage)`
  font-size: ${({ theme }) => theme.font.sm};
  margin: 0;
`;

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
  padding: 40px;
  width: 100%;
  cursor: pointer;
`;

interface PropsT {
  loading: boolean;
  success: boolean;
  error: boolean;
  onClose: () => void;
  onUpload: (query: ExternalQueryT) => void;
}

const UploadQueryResultsModal: FC<PropsT> = ({
  loading,
  success,
  error,
  onClose,
  onUpload,
}) => {
  const { t } = useTranslation();
  const [file, setFile] = useState<File | null>(null);

  return (
    <Modal
      onClose={onClose}
      closeIcon
      headline={
        <>
          {t("uploadQueryResultsModal.headline")}
          <InfoTooltip text={t("uploadQueryResultsModal.formatInfo.text")} />
        </>
      }
    >
      <Root>
        {success ? (
          <Success>
            <StyledFaIcon icon="check-circle" />
            <SuccessMsg>
              {t("uploadQueryResultsModal.uploadSucceeded")}
            </SuccessMsg>
          </Success>
        ) : (
          <div>
            {file && (
              <CSVColumnPicker
                file={file}
                loading={loading}
                onUpload={onUpload}
                onReset={() => setFile(null)}
              />
            )}
            {!file && (
              <SxDropzoneWithFileInput
                onDrop={(item) => {
                  if (item.files) {
                    setFile(item.files[0]);
                  }
                }}
                onSelectFile={setFile}
              >
                {() => t("uploadQueryResultsModal.dropzone")}
              </SxDropzoneWithFileInput>
            )}
            {error && (
              <Error>
                <ErrorMessage
                  message={t("uploadQueryResultsModal.uploadFailed")}
                />
                <ErrorMessageSub
                  message={t("uploadQueryResultsModal.uploadFailedSub")}
                />
              </Error>
            )}
          </div>
        )}
      </Root>
    </Modal>
  );
};

export default UploadQueryResultsModal;
