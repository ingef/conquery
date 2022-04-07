import Papa, { ParseResult } from "papaparse";

export function parseCSV(file: File, delimiter?: string) {
  return new Promise<ParseResult<string[]>>((resolve) => {
    Papa.parse<string[]>(file, {
      header: false,
      delimiter: delimiter || ";",
      skipEmptyLines: true,
      complete: (results) => resolve(results),
    });
  });
}

export function loadCSV(url: string): Promise<ParseResult<string[]>> {
  return new Promise((resolve) => {
    Papa.parse<string[]>(url, {
      download: true,
      delimiter: ";",
      skipEmptyLines: true,
      complete: (results) => resolve(results),
    });
  });
}

export function toCSV(data: string[][], delimiter: string = ";") {
  return Papa.unparse(data, { delimiter, newline: "\r\n" });
}
