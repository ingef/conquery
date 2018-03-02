// @flow

import React                   from 'react';

import { type FieldPropsType } from 'redux-form';

import {
  PREVIOUS_QUERY,
} from '../../common/constants/dndTypes';


import { Dropzone }       from '../../form-components';
import FormQueryResult    from './FormQueryResult';

type PropsType = FieldPropsType & {
  label: string,
  dropzoneText: string,
  className?: string,
};

export const FormQueryDropzone = (props: PropsType) => {
  const onDrop = (dropzoneProps, monitor) => {
    const item = monitor.getItem();

    props.input.onChange(item);
  };

  const QueryDropzone = Dropzone((
    <FormQueryResult
      className="externalForms__query-result"
      queryResult={props.input.value}
      onDelete={() => props.input.onChange(null)}
    />
  ), [PREVIOUS_QUERY], onDrop);

  return (
    <div className={props.className}>
      <span className="input-label">
        { props.label }
      </span>
      <QueryDropzone
        className="externalForms__dropzone"
        containsItem={!!props.input.value}
        dropzoneText={props.dropzoneText}
      />
    </div>
  );
};
