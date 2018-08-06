// @flow

import React           from 'react';
import classnames      from 'classnames';

import CloseIconButton from '../button/CloseIconButton';
import Dropzone        from './Dropzone';


type PropsType = {
  className?: string,
  itemClassName?: string,
  dropzoneClassName?: string,
  label?: string,
  dropzoneText: string,
  items: any,
  acceptedDropTypes: string[],
  onDrop: Function,
  onDelete: Function,
  disallowMultipleColumns?: boolean
};

const DropzoneList = (props: PropsType) => {
  const FreeDropzone = Dropzone(null, props.acceptedDropTypes, props.onDrop);

  return (
    <div className={classnames("dropzone-list", props.className)}>
      {
        props.label &&
        <span className="input-label dropzone-list__label">
          { props.label }
        </span>
      }
      {
        props.items && props.items.length > 0 &&
        <div className="dropzone-list__items">
          {
            props.items.map((item, i) => (
              <div key={i} className={classnames("dropzone-list__item", props.itemClassName)}>
                <CloseIconButton
                  className="dropzone-list__remove-item-btn"
                  onClick={() => props.onDelete(i)}
                />
                {item}
              </div>
            ))
          }
        </div>
      }
      {
        // allow at least one column
        ((props.items && props.items.length === 0) || !props.disallowMultipleColumns) &&
        <FreeDropzone
          className={props.dropzoneClassName}
          containsItem={false}
          dropzoneText={props.dropzoneText}
        />
      }
    </div>
  );
};

export default DropzoneList;
