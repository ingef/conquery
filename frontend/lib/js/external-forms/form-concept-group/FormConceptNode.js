// @flow

import React               from 'react';
import classnames          from 'classnames';

import { T }               from '../../localization';
import IconButton          from '../../button/IconButton';

type PropsType = {
  valueIdx: number,
  conceptIdx: number,
  conceptNode: Object,
  className?: string,
  name: string,
  onFilterClick: Function,
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
        label={T.translate('externalForms.common.edit')}
        className={classnames(
          "btn--link-like",
          { "query-node-actions__action--active": props.hasActiveFilters }
        )}
        onClick={props.onFilterClick}
        iconClassName="fa-sliders"
      />
      { props.conceptNode && props.conceptNode.label }
    </div>
  );
};

export default FormConceptNode;
