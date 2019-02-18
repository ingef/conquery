// @flow

import React                from 'react';
import T                    from 'i18n-react';
import { CloseIconButton }  from '../button';

type PropsType = {
  excludeActiveClass: string,
  dateActiveClass: string,
  onExcludeClick: () => void,
  onDeleteGroup: () => void,
  onDateClick: () => void,
};

const QueryGroupActions = (props: PropsType) => {
  return (
    <div className="query-group__actions">
      <div className="query-group__actions--left">
        <span
          className={
            `query-group__action ${props.excludeActiveClass} btn--icon query-group__exclude-btn`
          }
          onClick={props.onExcludeClick}
        >
          <i
            className="query-group__exclude-icon fa fa-ban"
          /> {T.translate('queryEditor.exclude')}
        </span>
        <span
          className={
            `query-group__action ${props.dateActiveClass} btn--icon query-group__date-btn`
          }
          onClick={props.onDateClick}
        >
          <i
            className="query-group__date-icon fa fa-calendar-o"
          /> {T.translate('queryEditor.date')}
        </span>
      </div>
      <div className="query-group__actions--right">
        <CloseIconButton
          className="query-group__action"
          onClick={props.onDeleteGroup}
        />
      </div>
    </div>
  );
};

export default QueryGroupActions;
