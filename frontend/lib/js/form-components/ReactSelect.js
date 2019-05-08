// @flow

import React from "react";
import Select from "react-select";
import Creatable from "react-select/lib/Creatable";

// TODO: Support value-changed
//       border: 1px solid $col-blue-gray-dark !important

// Helps to have a common ground for styling selects
const styles = {
  control: (provided, state) => ({
    ...provided,
    fontSize: "14px",
    borderRadius: "3px",
    boxShadow: "none",
    backgroundColor: "white",
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

export default React.forwardRef(({ creatable, ...props }, ref) => {
  return creatable ? (
    <Creatable ref={ref} styles={styles} {...props} />
  ) : (
    <Select ref={ref} styles={styles} {...props} />
  );
});
