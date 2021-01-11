import * as React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import InfoTooltip from "../../tooltip/InfoTooltip";

import DropzoneWithFileInput from "../../form-components/DropzoneWithFileInput";

import Modal from "../../modal/Modal";
import ErrorMessage from "../../error-message/ErrorMessage";
import FaIcon from "../../icon/FaIcon";

import CSVColumnPicker, { ExternalQueryT } from "./CSVColumnPicker";
import { DropTargetMonitor } from "react-dnd";

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
  success: Object | null;
  error: Object | null;
  onClose: () => void;
  onUpload: (query: ExternalQueryT) => void;
}

const UploadQueryResultsModal: React.FC<PropsT> = ({
  loading,
  success,
  error,
  onClose,
  onUpload,
}) => {
  const [file, setFile] = React.useState<File | null>(null);

  function onDrop(_: any, monitor: DropTargetMonitor) {
    const item = monitor.getItem();

    if (item.files) {
      setFile(item.files[0]);
    }
  }

  return (
    <Modal
      onClose={onClose}
      closeIcon
      headline={
        <>
          {T.translate("uploadQueryResultsModal.headline")}
          <InfoTooltip
            text={T.translate("uploadQueryResultsModal.formatInfo.text")}
          />
        </>
      }
    >
      <Root>
        {success ? (
          <Success>
            <StyledFaIcon icon="check-circle" />
            <SuccessMsg>
              {T.translate("uploadQueryResultsModal.uploadSucceeded")}
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
              <SxDropzoneWithFileInput onDrop={onDrop} onSelectFile={setFile}>
                {() => T.translate("uploadQueryResultsModal.dropzone")}
              </SxDropzoneWithFileInput>
            )}
            {error && (
              <Error>
                <ErrorMessage
                  message={T.translate("uploadQueryResultsModal.uploadFailed")}
                />
                <ErrorMessageSub
                  message={T.translate(
                    "uploadQueryResultsModal.uploadFailedSub"
                  )}
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
