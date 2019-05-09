// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { NativeTypes } from "react-dnd-html5-backend";

import { InfoTooltip } from "../../tooltip";

import Dropzone from "../../form-components/Dropzone";

import { Modal } from "../../modal";
import { ErrorMessage } from "../../error-message";
import FaIcon from "../../icon/FaIcon";
import IconButton from "../../button/IconButton";
import PrimaryButton from "../../button/PrimaryButton";

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

const StyledIconButton = styled(IconButton)`
  margin-left: 10px;
`;

const StyledDropzone = styled(Dropzone)`
  padding: 40px;
  width: 100%;
  margin: 0 0 20px;
  cursor: pointer;
`;

const FileInput = styled("input")`
  display: none;
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

const DROP_TYPES = [NativeTypes.FILE];

class UploadQueryResultsModal extends React.Component<PropsType, StateType> {
  props: PropsType;
  state: StateType = {
    file: null
  };

  constructor(props) {
    super(props);

    this.fileInputRef = React.createRef();
  }

  onDrop = (_, monitor) => {
    const item = monitor.getItem();

    if (item.files) {
      this.setState({ file: item.files[0] });
    }
  };

  onSelectFile = file => {
    this.setState({ file: this.fileInputRef.current.files[0] });
  };

  onOpenFileDialog = () => {
    this.fileInputRef.current.click();
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
                <StyledDropzone
                  acceptedDropTypes={DROP_TYPES}
                  onDrop={this.onDrop}
                  onClick={this.onOpenFileDialog}
                >
                  <FileInput
                    ref={this.fileInputRef}
                    type="file"
                    onChange={this.onSelectFile}
                  />
                  {T.translate("uploadQueryResultsModal.dropzone")}
                </StyledDropzone>
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
