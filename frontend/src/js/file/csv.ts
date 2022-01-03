import Papa, { ParseResult } from "papaparse";

export function parseCSV(file: File, delimiter?: string) {
  return new Promise<{ result: ParseResult<string[]>; file?: File }>(
    (resolve) => {
      Papa.parse<string[]>(file, {
        header: false,
        delimiter: delimiter || ";",
        skipEmptyLines: true,
        complete: (results, file) =>
          resolve({
            result: results,
            file,
          }),
      });
    },
  );
}

export function loadCSV(
  url: string,
): Promise<{ result: ParseResult<string[]>; file?: File }> {
  return new Promise((resolve) => {
    Papa.parse<string[]>(url, {
      download: true,
      delimiter: ";",
      skipEmptyLines: true,
      complete: (results, file) =>
        resolve({
          result: results,
          file,
        }),
    });
  });
}

export function toCSV(data: string[][], delimiter: string = ";") {
  return Papa.unparse(data, { delimiter, newline: "\r\n" });
}
