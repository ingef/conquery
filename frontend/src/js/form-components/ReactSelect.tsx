import { Theme, useTheme } from "@emotion/react";
import React from "react";
import Select, { Props } from "react-select";
import Creatable, { Props as CreatableProps } from "react-select/creatable";

import type { SelectOptionT } from "../api/types";

// Helps to have a common ground for styling selects
const stylesFromTheme = (theme: Theme, changed?: boolean, small?: boolean) => ({
  control: (provided: any, state: any) => {
    const smallStyles = small
      ? {
          minHeight: "0",
        }
      : {};

    return {
      ...provided,
      ...smallStyles,
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
      padding: small ? "3px" : "6px",
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
  small?: boolean;
}

interface CreatablePropsT<IsMulti extends boolean>
  extends CreatableProps<SelectOptionT, IsMulti> {
  creatable: true;
  highlightChanged?: boolean;
  small?: boolean;
}

type PropsT<IsMulti extends boolean> =
  | CreatablePropsT<IsMulti>
  | SelectPropsT<IsMulti>;

const ReactSelect = <IsMulti extends boolean>({
  creatable,
  highlightChanged,
  small,
  ...props
}: PropsT<IsMulti>) => {
  const theme = useTheme();
  const hasChanged =
    JSON.stringify(props.value) !== JSON.stringify(props.defaultValue);
  const changed = hasChanged && highlightChanged;

  const styles = stylesFromTheme(theme, changed, small);

  return creatable ? (
    <Creatable styles={styles} {...props} />
  ) : (
    <Select styles={styles} {...props} />
  );
};

export default ReactSelect;
