import React                from 'react';
import PropTypes            from 'prop-types';
import { connect }          from 'react-redux';
import classnames           from 'classnames';

const SelectableLabel = (props) => {
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

SelectableLabel.propTypes = {
  className: PropTypes.string,
  label: PropTypes.string.isRequired,
  isSelected: PropTypes.bool.isRequired,
};

const labelContainsAnySearch = (label, searches) => {
  return searches.some(search => label.toLowerCase().indexOf(search.toLowerCase()) !== -1);
}

const mapStateToProps = (state, ownProps) => ({
  isSelected: labelContainsAnySearch(ownProps.label, state.previousQueriesSearch),
});

export default connect(mapStateToProps)(SelectableLabel);
