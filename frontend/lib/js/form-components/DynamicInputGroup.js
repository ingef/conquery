import React                from 'react';
import classnames           from 'classnames';

import CloseIconButton      from '../button/CloseIconButton';
import IconButton           from '../button/IconButton';
import { ExpandButton } from '../button';

type PropsType = {
  className?: string,
  label?: string,
  items: Array<Element>,
  onAddClick: Function,
  onRemoveClick: Function,
  canExpand: boolean,
};

const DynamicInputGroup = (props: PropsType) => (
  <div className={classnames("dynamic-input-group", props.className)}>
    {
      props.label &&
      <span className="input-label dynamic-input-group__label">
        { props.label }
      </span>
    }
    {
      props.items.map((item, idx) => (
        <div key={idx} className="dynamic-input-group__item">
          { item }
          <CloseIconButton
            className="dynamic-input-group__item__remove-btn"
            onClick={() => props.onRemoveClick(idx)}
          />
        </div>
      ))
    }
    <IconButton
      className="btn--icon btn--transparent btn--small dynamic-input-group__add-btn"
      iconClassName="fa-plus"
      type="button"
      onClick={props.onAddClick}
    />
  </div>
);

export default DynamicInputGroup;
