// @flow

import Papa from "papaparse";

export default function parseCSV(file: File, delimiter?: string) {
  return new Promise((resolve, reject) => {
    Papa.parse(file, {
      header: false,
      delimiter: delimiter || ";",
      skipEmptyLines: true,
      complete: (results, file) => resolve(results, file)
    });
  });
}
