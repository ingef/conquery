import { Theme, useTheme } from "@emotion/react";
import React from "react";
import Select, { Props } from "react-select";
import Creatable, { Props as CreatableProps } from "react-select/creatable";

import type { SelectOptionT } from "../api/types";

// Helps to have a common ground for styling selects
const stylesFromTheme = (theme: Theme, changed?: boolean) => ({
  control: (provided: any, state: any) => {
    return {
      ...provided,
      minHeight: "30px", // makes it a little bit smaller in height than usual
      fontSize: theme.font.sm,
      borderRadius: "3px",
      boxShadow: "none",
      backgroundColor: "white",
      borderColor: changed && state.hasValue ? theme.col.blueGrayDark : "#aaa",
      ":hover": {
        borderColor:
          changed && state.hasValue ? theme.col.blueGrayDark : "#aaa",
      },
    };
  },
  dropdownIndicator: (provided: any, state: any) => {
    return {
      ...provided,
      padding: "3px",
    };
  },
  option: (provided: any, state: any) => {
    return {
      ...provided,
      cursor: "pointer",
      fontSize: "14px",
      color: state.isSelected ? "white" : theme.col.black,
      fontWeight: state.isSelected ? 400 : 300,
      backgroundColor: state.isSelected
        ? theme.col.blueGrayDark
        : state.isFocused
        ? theme.col.blueGrayVeryLight
        : "white",
      ":active": {
        backgroundColor: theme.col.blueGrayLight,
        color: "white",
      },
    };
  },
  multiValueRemove: (provided: any) => ({
    ...provided,
    cursor: "pointer",
    ":hover": {
      backgroundColor: theme.col.gray,
    },
  }),
});

interface SelectPropsT<IsMulti extends boolean>
  extends Props<SelectOptionT, IsMulti> {
  highlightChanged?: boolean;
}

interface CreatablePropsT<IsMulti extends boolean>
  extends CreatableProps<SelectOptionT, IsMulti> {
  creatable: true;
  highlightChanged?: boolean;
}

type PropsT<IsMulti extends boolean> =
  | CreatablePropsT<IsMulti>
  | SelectPropsT<IsMulti>;

const ReactSelect = <IsMulti extends boolean>({
  creatable,
  highlightChanged,
  ...props
}: PropsT<IsMulti>) => {
  const theme = useTheme();
  const hasChanged =
    JSON.stringify(props.value) !== JSON.stringify(props.defaultValue);
  const changed = hasChanged && highlightChanged;

  const styles = stylesFromTheme(theme, changed);

  return creatable ? (
    <Creatable styles={styles} {...props} />
  ) : (
    <Select styles={styles} {...props} />
  );
};

export default ReactSelect;
