import React, { FC, ReactNode } from "react";
import type { WrappedFieldProps } from "redux-form";

import {
  PREVIOUS_QUERY,
  PREVIOUS_SECONDARY_ID_QUERY,
} from "../../common/constants/dndTypes";
import { PreviousQueryT } from "../../previous-queries/list/reducer";
import type { DragItemQuery } from "../../standard-query-editor/types";
import type { ChildArgs } from "../../ui-components/Dropzone";
import DropzoneList from "../form-components/DropzoneList";

import FormQueryResult from "./FormQueryResult";

interface PropsT extends WrappedFieldProps {
  dropzoneChildren: (args: ChildArgs) => ReactNode;
  label: string;
  tooltip?: string;
  optional?: boolean;
}

const FormMultiQueryDropzone: FC<PropsT> = ({
  input,
  label,
  tooltip,
  optional,
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
    <DropzoneList<DragItemQuery>
      acceptedDropTypes={[PREVIOUS_QUERY, PREVIOUS_SECONDARY_ID_QUERY]}
      label={label}
      optional={optional}
      tooltip={tooltip}
      dropzoneChildren={dropzoneChildren}
      items={input.value.map((query: PreviousQueryT, i: number) => (
        <FormQueryResult key={i} queryResult={query} />
      ))}
      onDrop={(item) => {
        return input.onChange(addValue(item));
      }}
      onDelete={(i: number) => removeValue(i)}
    ></DropzoneList>
  );
};

export default FormMultiQueryDropzone;
