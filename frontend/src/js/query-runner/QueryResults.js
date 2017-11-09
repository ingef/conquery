// @flow

import React              from 'react';
import T                  from 'i18n-react';

import { DownloadButton } from '../button';

type PropsType = {
  resultCount: number,
  resultUrl: string,
};

const QueryResults = (props: PropsType) => {
  if (typeof props.resultCount === 'undefined') return null;

  return (
    <div className="query-results">
      <p className="query-results__text">
        {props.resultCount}
        {T.translate('queryRunner.resultCount')}
      </p>
      {
        props.resultCount > 0 && props.resultUrl &&
        <DownloadButton
          className="query-results__download-btn btn btn--small btn--primary"
          label={T.translate('queryRunner.downloadResults')}
          url={props.resultUrl}
        />
      }
    </div>
  );
};


export default QueryResults;
