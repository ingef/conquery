// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import ReactDropzone from "react-dropzone";

import { InfoTooltip } from "../../tooltip";

import { Modal } from "../../modal";
import { ErrorMessage } from "../../error-message";
import FaIcon from "../../icon/FaIcon";
import IconButton from "../../button/IconButton";
import PrimaryButton from "../../button/PrimaryButton";

const Root = styled("div")`
  padding: 20px 0 10px;
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

const StyledIconButton = styled(IconButton)`
  margin-left: 10px;
`;

type PropsType = {
  onCloseModal: Function,
  onUploadFile: Function,
  loading: boolean,
  success: ?Object,
  error: ?Object
};

type StateType = {
  file: any
};

class UploadQueryResultsModal extends React.Component<PropsType, StateType> {
  props: PropsType;
  state: StateType = {
    file: null
  };

  onDrop = (acceptedFiles: any) => {
    this.setState({ file: acceptedFiles[0] });
  };

  onReset = () => {
    this.setState({ file: null });
  };

  render() {
    return (
      <Modal
        closeModal={this.props.onCloseModal}
        doneButton
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
          {this.props.success && (
            <Success>
              <StyledFaIcon icon="check-circle" />
              <SuccessMsg>
                {T.translate("uploadQueryResultsModal.previousQueryCreated")}
              </SuccessMsg>
            </Success>
          )}
          {!this.props.success && (
            <div>
              {this.state.file ? (
                <p>
                  {this.state.file.name}
                  <StyledIconButton
                    frame
                    regular
                    icon="trash-alt"
                    onClick={this.onReset}
                  />
                </p>
              ) : (
                <ReactDropzone
                  onDrop={this.onDrop}
                  className="upload-query-results-modal__dropzone"
                  activeClassName="upload-query-results-modal__dropzone--accepting"
                  rejectClassName="upload-query-results-modal__dropzone--rejecting"
                >
                  {T.translate("uploadQueryResultsModal.dropzone")}
                </ReactDropzone>
              )}
              {this.props.error && (
                <Error>
                  <ErrorMessage
                    message={T.translate(
                      "uploadQueryResultsModal.uploadFailed"
                    )}
                  />
                  <ErrorMessageSub
                    message={T.translate(
                      "uploadQueryResultsModal.uploadFailedSub"
                    )}
                  />
                </Error>
              )}
              <PrimaryButton
                disabled={!this.state.file || this.props.loading}
                onClick={() => this.props.onUploadFile(this.state.file)}
              >
                {this.props.loading && <FaIcon white icon="spinner" />}{" "}
                {T.translate("uploadQueryResultsModal.upload")}
              </PrimaryButton>
            </div>
          )}
        </Root>
      </Modal>
    );
  }
}

export default UploadQueryResultsModal;
