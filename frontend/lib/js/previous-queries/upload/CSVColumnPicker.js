// @flow

import * as React from "react";
import parseCSV from "../../file/parseCSV";

type PropsType = {
  file: File,
  onUpload: query => void
};

export default ({ file, onUpload }) => {
  const [csv, setCSV] = React.useState([]);

  React.useEffect(() => {
    async function parse(f) {
      const result = await parseCSV(f);

      setCSV(result);
    }

    parse(file);
  }, [file]);

  return <div>{file.name}</div>;
};
