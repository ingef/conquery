// @flow

import React from "react";
import Select from "react-select";
import Creatable from "react-select/lib/Creatable";

// Helps to have a common ground for styling selects
const styles = {
  control: (provided, state) => ({
    ...provided,
    fontSize: "14px",
    borderRadius: "3px",
    backgroundColor: "white",
    boxShadow: "transparent",
    borderColor: "#aaa",
    ":hover": {
      borderColor: "#aaa"
    }
  }),
  option: provided => ({
    ...provided,
    cursor: "pointer",
    fontSize: "14px"
  }),
  multiValueRemove: provided => ({
    ...provided,
    cursor: "pointer"
  })
};

export default ({ creatable, ...props }) => {
  return creatable ? (
    <Creatable styles={styles} {...props} />
  ) : (
    <Select styles={styles} {...props} />
  );
};
