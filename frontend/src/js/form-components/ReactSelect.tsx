import React from "react";
import Select from "react-select";
import Creatable from "react-select/creatable";
import { withTheme } from "@emotion/react";

// Helps to have a common ground for styling selects
const stylesFromTheme = (theme, changed, small) => ({
  control: (provided, state) => {
    const smallStyles = small
      ? {
          minHeight: "0"
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
        borderColor: changed && state.hasValue ? theme.col.blueGrayDark : "#aaa"
      }
    };
  },
  dropdownIndicator: (provided, state) => {
    return {
      ...provided,
      padding: small ? "3px" : "6px"
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

const ReactSelect = ({
  theme,
  creatable,
  highlightChanged,
  small,
  ...props
}) => {
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

export default withTheme(ReactSelect);
