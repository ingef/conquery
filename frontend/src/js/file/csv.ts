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

export function loadCSV(
  url: string,
  { english }: { english?: boolean } = {},
): Promise<ParseResult<string[]>> {
  return new Promise((resolve) => {
    const downloadRequestHeaders = english
      ? {
          downloadRequestHeaders: {
            // Because we support different csv header versions depending on language
            "Accept-Language": "en-US,en",
          },
        }
      : {};

    Papa.parse<string[]>(url, {
      ...downloadRequestHeaders,
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

export function parseCSVWithHeaderToObj(csv: string, delimiter?: string) {
  return new Promise<{ [key: string]: any }[]>((resolve) => {
    Papa.parse<{ [key: string]: any }[]>(csv, {
      header: true,
      download: false,
      delimiter: delimiter || ";",
      skipEmptyLines: true,
      complete: (results) =>
        resolve(
          results.data.map((row) =>
            Object.fromEntries(
              Object.entries(row).filter(([_, value]) => !!value),
            ),
          ),
        ),
    });
  });
}
