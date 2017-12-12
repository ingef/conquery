// @flow

import React               from 'react';
import { connect }         from 'react-redux';
import type { Dispatch }   from 'redux-thunk';
import classnames          from 'classnames';
import T                   from 'i18n-react';

import IconButton          from '../../button/IconButton';

type PropsType = {
  valueIdx: number,
  conceptIdx: number,
  conceptNode: Object,
  className?: string,
  name: string,
  onFilterClick: Function,
  setFeaturesNodeAction: Function,
  setOutcomesNodeAction: Function,
  hasActiveFilters: boolean,
};

// generalized node to handle concepts queried in forms
const FormConceptNode = (props: PropsType) => {
  return (
    <div className={classnames(
      'select-override--tiny',
      props.className
    )}>
      <IconButton
        label={T.translate('externalForms.common.filter')}
        className={classnames(
          "btn--link-like",
          { "query-node-actions__action--active": props.hasActiveFilters }
        )}
        onClick={() => props.onFilterClick(props.name, props.valueIdx, props.conceptIdx)}
        iconClassName="fa-sliders"
      />
      { props.conceptNode && props.conceptNode.label }
    </div>
  );
};

const mapDispatchToProps = (dispatch: Dispatch, ownProps: Object) => ({
  onFilterClick: (name, valueIdx, conceptIdx) => {
    if (name === 'features')
      dispatch(ownProps.setFeaturesNodeAction(valueIdx, conceptIdx));
    else if (name === 'outcomes')
      dispatch(ownProps.setOutcomesNodeAction(valueIdx, conceptIdx));
  }
});

export default connect(() => ({}), mapDispatchToProps)(FormConceptNode);
