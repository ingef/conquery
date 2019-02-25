// @flow

import React                from 'react';
import { connect }          from 'react-redux';
import classnames           from 'classnames';

type PropsType = {
  className: string,
  label: string,
  isSelected: boolean,
};

const SelectableLabel = (props: PropsType) => {
  return (
    <span className={classnames(
      props.className,
       "selectable-label", {
         "selectable-label--selected": props.isSelected
       }
    )}>
      {props.label}
    </span>
  );
};

const labelContainsAnySearch = (label, searches) => {
  return searches.some(search => label.toLowerCase().indexOf(search.toLowerCase()) !== -1);
}

const mapStateToProps = (state, ownProps) => ({
  isSelected: labelContainsAnySearch(ownProps.label, state.previousQueriesSearch),
});

export default connect(mapStateToProps)(SelectableLabel);
