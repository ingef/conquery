// @flow

import React                     from 'react';
import T                         from 'i18n-react';
import ReactDropzone             from 'react-dropzone';

import { InfoTooltip }           from '../../tooltip';

import { Modal }                 from '../../modal';
import { ErrorMessage }          from '../../error-message';
import { CloseIconButton }       from '../../button';
import UploadReport              from './UploadReport';
import { type UploadReportType } from './reducer';


type PropsType = {
  onCloseModal: Function,
  onUploadFile: Function,
  loading: boolean,
  success: ?UploadReportType,
  error: ?(UploadReportType & { message: string }),
};

type StateType = {
  file: any,
  error: any
};

class UploadQueryResultsModal extends React.Component {
  props: PropsType;
  state: StateType;

  constructor(props: PropsType) {
    super(props);

    (this:any).state = {
      file: null,
      error: props.error
    };
  }

  componentWillReceiveProps(nextProps) {
    this.setState({error: nextProps.error})
  }

  _onDrop(acceptedFiles: any) {
    this.setState({ file: acceptedFiles[0] });
  }

  _onReset() {
    this.setState({ file: null, error: null });
  }

  render() {
    return (
      <Modal closeModal={this.props.onCloseModal} doneButton>
        <div className="upload-query-results-modal">
          <InfoTooltip
            text={T.translate('uploadQueryResultsModal.formatInfo.text')}
          />
          <h3 className="upload-query-results-modal__headline" >
            { T.translate('uploadQueryResultsModal.headline') }
          </h3>
          {
            this.props.success &&
            <div className="upload-query-results-modal__success">
              <p><i className="fa fa-check-circle fa-5x" /></p>
              <p>
                {T.translate('uploadQueryResultsModal.previousQueryCreated')}
              </p>
              <UploadReport report={this.props.success} />
            </div>
          }
          {
            !this.props.success &&
            <div>
              {
                this.state.file
                  ? <p>
                      <CloseIconButton
                        onClick={this._onReset.bind(this)}
                      />
                      {this.state.file.name}
                    </p>
                  : <ReactDropzone
                      onDrop={this._onDrop.bind(this)}
                      className="upload-query-results-modal__dropzone"
                      activeClassName="upload-query-results-modal__dropzone--accepting"
                      rejectClassName="upload-query-results-modal__dropzone--rejecting"
                    >
                      {T.translate('uploadQueryResultsModal.dropzone')}
                    </ReactDropzone>
              }
              {
                this.props.error && this.state.error &&
                  <div className="upload-query-results-modal__error">
                    <ErrorMessage
                      message={this.props.error.message}
                    />
                    <UploadReport report={this.props.error} />
                    {
                      this.props.error.details &&
                        <div className="upload-query-results-modal__details">
                          <textarea
                            rows={4}
                            cols={30}
                            value={this.props.error.details.message}
                            disabled={true}
                          />
                        </div>
                    }
                  </div>
              }
              <button
                type="button"
                className="btn btn--primary"
                disabled={!this.state.file}
                onClick={() => this.props.onUploadFile(this.state.file)}
              >
                {
                  this.props.loading &&
                  <i className="fa fa-spinner" />
                }  { T.translate('uploadQueryResultsModal.upload') }
              </button>
            </div>
          }
        </div>
      </Modal>
    );
  }
};

export default UploadQueryResultsModal;
