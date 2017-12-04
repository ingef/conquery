// @flow

import React                from 'react';
import T                    from 'i18n-react';
import classnames           from 'classnames';
import { CloseIconButton }  from '../button';


type PropsType = {
  hasActiveFilters?: boolean,
  isExpandable?: boolean,
  hasDetails?: boolean,
  previousQueryLoading?: boolean,
  error?: string,
  onDeleteNode: Function,
  onFilterClick: Function,
  onExpandClick: Function,
  onDetailsClick: Function,
};

const QueryNodeActions = (props: PropsType) => {
  const base = "query-node-actions";

  return (
    <div className={`${base}`}>
      {
        !props.error &&
        <div className={`${base}--left`}>
          {
            !props.previousQueryLoading &&
            <span
              onClick={props.onFilterClick}
              className={classnames(
                'btn--icon',
                `${base}__action`,
                {
                  [`${base}__action--active`]: props.hasActiveFilters
                }
              )}
            >
              <i
                className="fa fa-sliders"
              /> {T.translate('queryEditor.filter')}
            </span>
          }
          {
            !!props.previousQueryLoading &&
            <span className={`${base}__loading`}>
              <i className="fa fa-spinner" /> {T.translate('queryEditor.loadingPreviousQuery')}
            </span>

          }
          {
            props.isExpandable && !props.previousQueryLoading &&
            <span
              onClick={props.onExpandClick}
              className={`btn--icon ${base}__action`}
            >
              <i className="fa fa-expand" /> {T.translate('queryEditor.expand')}
            </span>
          }
          {
            props.hasDetails &&
            <span
              onClick={props.onDetailsClick}
              className={`btn--icon ${base}__action`}
            >
              <i className="fa fa-list-alt" /> {T.translate('queryEditor.details')}
            </span>
          }
        </div>
      }
      <div className={`${base}--right`}>
        <CloseIconButton
          className={`${base}__action`}
          onClick={props.onDeleteNode}
        />
      </div>
    </div>
  );
};

export default QueryNodeActions;
