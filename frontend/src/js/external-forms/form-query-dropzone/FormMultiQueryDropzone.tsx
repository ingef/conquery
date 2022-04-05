import { FC, ReactNode } from "react";

import { DNDType } from "../../common/constants/dndTypes";
import type { DragItemQuery } from "../../standard-query-editor/types";
import type { ChildArgs } from "../../ui-components/Dropzone";
import DropzoneList from "../form-components/DropzoneList";

import FormQueryResult from "./FormQueryResult";

interface PropsT {
  dropzoneChildren: (args: ChildArgs) => ReactNode;
  label: string;
  tooltip?: string;
  optional?: boolean;
  value: DragItemQuery[];
  onChange: (value: DragItemQuery[]) => void;
}

const DROP_TYPES = [
  DNDType.PREVIOUS_QUERY,
  DNDType.PREVIOUS_SECONDARY_ID_QUERY,
];

const FormMultiQueryDropzone: FC<PropsT> = ({
  label,
  tooltip,
  optional,
  dropzoneChildren,
  value,
  onChange,
}) => {
  const addValue = (newItem: DragItemQuery) => {
    onChange([...value, newItem]);
  };

  const removeValue = (valueIdx: number) => {
    onChange([...value.slice(0, valueIdx), ...value.slice(valueIdx + 1)]);
  };

  return (
    <DropzoneList<DragItemQuery>
      acceptedDropTypes={DROP_TYPES}
      label={label}
      optional={optional}
      tooltip={tooltip}
      dropzoneChildren={dropzoneChildren}
      onDropFile={undefined}
      items={value.map((query: DragItemQuery, i: number) => (
        <FormQueryResult key={i} queryResult={query} />
      ))}
      onDrop={(item) => addValue(item)}
      onDelete={(i: number) => removeValue(i)}
    ></DropzoneList>
  );
};

export default FormMultiQueryDropzone;
