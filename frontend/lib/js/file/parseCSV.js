import Papa from "papaparse";

export default function parseCSV(file) {
  return new Promise((resolve, reject) => {
    Papa.parse(file, {
      header: true,
      skipEmptyLines: true,
      complete: (results, file) => resolve(results, file)
    });
  });
}
