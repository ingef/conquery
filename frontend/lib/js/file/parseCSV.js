import Papa from "papaparse";

export default function parseCSV(file) {
  return new Promise((resolve, reject) => {
    Papa.parse(file, {
      header: false,
      skipEmptyLines: true,
      delimitersToGuess: [";", ",", ":"],
      complete: (results, file) => resolve(results, file)
    });
  });
}
