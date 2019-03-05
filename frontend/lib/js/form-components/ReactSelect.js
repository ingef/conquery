// @flow

import React from "react";
import Select from "react-select";
import Creatable from "react-select/lib/Creatable";

// Helps to have a common ground for styling selects
const styles = {
  control: provided => ({ ...provided, backgroundColor: "white" }),
  option: provided => ({
    ...provided,
    cursor: "pointer"
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
