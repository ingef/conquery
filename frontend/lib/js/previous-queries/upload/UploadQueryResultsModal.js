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
import UploadReport from "./UploadReport";
import { type UploadReportType } from "./reducer";

const Root = styled("div")`
  padding: 20px 0 10px;
  text-align: center;
`;

const Error = styled("div")`
  font-size: $font-sm;
`;

const Success = styled("div")`
  color: ${({ theme }) => theme.col.green};
`;

const StyledFaIcon = styled(FaIcon)`
  font-size: 40px;
`;

type PropsType = {
  onCloseModal: Function,
  onUploadFile: Function,
  loading: boolean,
  success: ?UploadReportType,
  error: ?(UploadReportType & { message: string })
};

type StateType = {
  file: any
};

class UploadQueryResultsModal extends React.Component<PropsType, StateType> {
  props: PropsType;
  state: StateType;

  constructor(props: PropsType) {
    super(props);

    (this: any).state = {
      file: null
    };
  }

  _onDrop(acceptedFiles: any) {
    this.setState({ file: acceptedFiles[0] });
  }

  _onReset() {
    this.setState({ file: null });
  }

  render() {
    return (
      <Modal closeModal={this.props.onCloseModal} doneButton>
        <Root>
          <InfoTooltip
            text={T.translate("uploadQueryResultsModal.formatInfo.text")}
          />
          <h3>{T.translate("uploadQueryResultsModal.headline")}</h3>
          {this.props.success && (
            <Success>
              <p>
                <StyledFaIcon icon="check-circle" />
              </p>
              <p>
                {T.translate("uploadQueryResultsModal.previousQueryCreated")}
              </p>
              <UploadReport report={this.props.success} />
            </Success>
          )}
          {!this.props.success && (
            <div>
              {this.state.file ? (
                <p>
                  <IconButton icon="close" onClick={this._onReset.bind(this)} />
                  {this.state.file.name}
                </p>
              ) : (
                <ReactDropzone
                  onDrop={this._onDrop.bind(this)}
                  className="upload-query-results-modal__dropzone"
                  activeClassName="upload-query-results-modal__dropzone--accepting"
                  rejectClassName="upload-query-results-modal__dropzone--rejecting"
                >
                  {T.translate("uploadQueryResultsModal.dropzone")}
                </ReactDropzone>
              )}
              {this.props.error && (
                <Error>
                  <ErrorMessage message={this.props.error.message} />
                  <UploadReport report={this.props.error} />
                </Error>
              )}
              <PrimaryButton
                disabled={!this.state.file}
                onClick={() => this.props.onUploadFile(this.state.file)}
              >
                {this.props.loading && <i className="fa fa-spinner" />}{" "}
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
