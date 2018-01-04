import React                from 'react';
import PropTypes            from 'prop-types';
import T                    from 'i18n-react';
import { CloseIconButton }  from '../button';

const QueryGroupActions = (props) => {
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

QueryGroupActions.propTypes = {
  excludeActiveClass: PropTypes.string,
  dateActiveClass: PropTypes.string,
  onExcludeClick: PropTypes.func.isRequired,
  onDeleteGroup: PropTypes.func.isRequired,
  onDateClick: PropTypes.func.isRequired,
};

export default QueryGroupActions;
