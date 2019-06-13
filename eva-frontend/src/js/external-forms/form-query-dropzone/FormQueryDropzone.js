// @flow

import React from "react";

import { type FieldPropsType } from "redux-form";

import FormQueryResult from "./FormQueryResult";

import { PREVIOUS_QUERY } from "conquery/lib/js/common/constants/dndTypes";

import Dropzone from "conquery/lib/js/form-components/Dropzone";
import Label from "conquery/lib/js/form-components/Label";

type PropsType = FieldPropsType & {
  label: string,
  dropzoneText: string,
  className?: string
};

export const FormQueryDropzone = (props: PropsType) => {
  const onDrop = (dropzoneProps, monitor) => {
    const item = monitor.getItem();

    props.input.onChange(item);
  };

  return (
    <div className={props.className}>
      <Label>{props.label}</Label>
      {!!props.input.value ? (
        <FormQueryResult
          className="externalForms__query-result"
          queryResult={props.input.value}
          onDelete={() => props.input.onChange(null)}
        />
      ) : (
        <Dropzone onDrop={onDrop} acceptedDropTypes={[PREVIOUS_QUERY]}>
          {props.dropzoneText}
        </Dropzone>
      )}
    </div>
  );
};
