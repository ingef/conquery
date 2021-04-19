import React, { FC, ReactNode } from "react";
import type { WrappedFieldProps } from "redux-form";

import {
  PREVIOUS_QUERY,
  PREVIOUS_SECONDARY_ID_QUERY,
} from "../../common/constants/dndTypes";
import type { ChildArgs } from "../../form-components/Dropzone";
import { PreviousQueryT } from "../../previous-queries/list/reducer";
import DropzoneList from "../form-components/DropzoneList";

import FormQueryResult from "./FormQueryResult";

interface PropsT extends WrappedFieldProps {
  dropzoneChildren: (args: ChildArgs) => ReactNode;
  label: string;
}

const FormMultiQueryDropzone: FC<PropsT> = ({
  input,
  label,
  dropzoneChildren,
}) => {
  const addValue = (newItem) => {
    input.onChange([...input.value, newItem]);
  };

  const removeValue = (valueIdx: number) => {
    input.onChange([
      ...input.value.slice(0, valueIdx),
      ...input.value.slice(valueIdx + 1),
    ]);
  };

  return (
    <DropzoneList
      acceptedDropTypes={[PREVIOUS_QUERY, PREVIOUS_SECONDARY_ID_QUERY]}
      label={label}
      dropzoneChildren={dropzoneChildren}
      items={input.value.map((query: PreviousQueryT, i: number) => (
        <FormQueryResult key={i} queryResult={query} />
      ))}
      onDrop={(dropzoneProps, monitor) => {
        const item = monitor.getItem();

        return input.onChange(addValue(item));
      }}
      onDelete={(i: number) => removeValue(i)}
    ></DropzoneList>
  );
};

export default FormMultiQueryDropzone;
