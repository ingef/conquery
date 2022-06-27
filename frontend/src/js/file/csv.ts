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
      downloadRequestHeaders: {
        // Because we support different csv header versions depending on language
        "Accept-Language": "en-US,en",
      },
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
