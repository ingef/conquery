import React, { FC } from "react";
import type { WrappedFieldProps } from "redux-form";

import {
  PREVIOUS_QUERY,
  PREVIOUS_SECONDARY_ID_QUERY,
} from "../../common/constants/dndTypes";
import Dropzone from "../../form-components/Dropzone";
import Label from "../../form-components/Label";
import type { DragItemQuery } from "../../standard-query-editor/types";

import FormQueryResult from "./FormQueryResult";

interface PropsT extends WrappedFieldProps {
  label: string;
  dropzoneText: string;
  className?: string;
}

const FormQueryDropzone: FC<PropsT> = (props) => {
  const onDrop = (item: DragItemQuery) => {
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
        <Dropzone<DragItemQuery>
          onDrop={onDrop}
          acceptedDropTypes={[PREVIOUS_QUERY, PREVIOUS_SECONDARY_ID_QUERY]}
        >
          {() => props.dropzoneText}
        </Dropzone>
      )}
    </div>
  );
};

export default FormQueryDropzone;
