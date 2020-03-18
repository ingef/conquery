// @flow

import * as React from "react";
import { type FieldPropsType } from "redux-form";

import { PREVIOUS_QUERY } from "../../common/constants/dndTypes";

import DropzoneList from "../form-components/DropzoneList";
import FormQueryResult from "./FormQueryResult";

type PropsT = FieldPropsType & {
  dropzoneText: string,
  label: string
};

export default ({ input, label, dropzoneText }: PropsT) => {
  const addValue = newItem => {
    input.onChange([...input.value, newItem]);
  };

  const removeValue = valueIdx => {
    input.onChange([
      ...input.value.slice(0, valueIdx),
      ...input.value.slice(valueIdx + 1)
    ]);
  };

  return (
    <DropzoneList
      acceptedDropTypes={[PREVIOUS_QUERY]}
      label={label}
      dropzoneText={dropzoneText}
      allowFile={false}
      items={input.value.map((concept, i) => (
        <FormQueryResult key={i} queryResult={concept} />
      ))}
      onDrop={(dropzoneProps, monitor) => {
        const item = monitor.getItem();

        return input.onChange(addValue(item));
      }}
      onDelete={i => removeValue(i)}
    ></DropzoneList>
  );
};
