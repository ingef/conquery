import React, { FC } from "react";

import type { WrappedFieldProps } from "redux-form";

import FormQueryResult from "./FormQueryResult";

import { PREVIOUS_QUERY } from "../../common/constants/dndTypes";

import Dropzone from "../../form-components/Dropzone";
import Label from "../../form-components/Label";

interface PropsT extends WrappedFieldProps {
  label: string;
  dropzoneText: string;
  className?: string;
}

const FormQueryDropzone: FC<PropsT> = (props) => {
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

export default FormQueryDropzone;
