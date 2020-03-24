import React from "react";

import type { FieldPropsType } from "redux-form";

import FormQueryResult from "./FormQueryResult";

import { PREVIOUS_QUERY } from "../../common/constants/dndTypes";

import Dropzone from "../../form-components/Dropzone";
import Label from "../../form-components/Label";

type PropsT = FieldPropsType & {
  label: string,
  dropzoneText: string,
  className?: string
};

export default (props: PropsT) => {
  const onDrop = (dropzoneProps, monitor) => {
    const item = monitor.getItem();

    props.input.onChange(item);
  };

  return (
    <div className={props.className}>
      <Label>{props.label}</Label>
      {!!props.input.value ? (
        <FormQueryResult
          queryResult={props.input.value}
          onDelete={() => props.input.onChange(null)}
        />
      ) : (
        <Dropzone onDrop={onDrop} acceptedDropTypes={[PREVIOUS_QUERY]}>
          {() => props.dropzoneText}
        </Dropzone>
      )}
    </div>
  );
};
