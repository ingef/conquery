// @flow

import React                     from 'react';
import T                         from 'i18n-react';

import { type UploadReportType } from './reducer';


type PropsType = {
  report: UploadReportType,
};

const UploadReport = (props: PropsType) => (
  <div className="upload-report">
    {
      props.report.successful > 0 &&
        <p className="upload-report__successful-count">
          {T.translate(
            'uploadReport.successful',
            { count: props.report.successful}
          )}
        </p>
    }
    {
      props.report.unsuccessful > 0 &&
      <p className="upload-report__unsuccessful-count">
        {T.translate(
          'uploadReport.unsuccessful',
          { count: props.report.unsuccessful }
        )}
      </p>
    }
  </div>
);

export default UploadReport;
