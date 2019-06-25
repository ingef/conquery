// @flow

import React from "react";
import Select from "react-select";
import Creatable from "react-select/lib/Creatable";
import { withTheme } from "emotion-theming";

// TODO: Support value-changed
//       border: 1px solid $col-blue-gray-dark !important

// Helps to have a common ground for styling selects
const stylesFromTheme = (theme, changed) => ({
  control: (provided, state) => {
    return {
      ...provided,
      fontSize: "14px",
      borderRadius: "3px",
      boxShadow: "none",
      backgroundColor: "white",
      borderColor: changed && state.hasValue ? theme.col.blueGrayDark : "#aaa",
      ":hover": {
        borderColor: changed && state.hasValue ? theme.col.blueGrayDark : "#aaa"
      }
    };
  },
  option: (provided, state) => {
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
        color: "white"
      }
    };
  },
  multiValueRemove: provided => ({
    ...provided,
    cursor: "pointer",
    ":hover": {
      backgroundColor: theme.col.gray
    }
  })
});

const ReactSelect = ({ theme, creatable, highlightChanged, ...props }) => {
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

export default withTheme(ReactSelect);
