export const readFileAsText = (file: File) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = (evt) => resolve(evt.target.result);
    reader.onerror = (err) => reject(err);

    reader.readAsText(file);
  });

export const cleanFileContent = (fileContent: string) => {
  return fileContent
    .split("\n")
    .map((row) => row.trim())
    .filter((row) => row.length > 0);
};

export const stripFilename = (fileName: string) => {
  return fileName.replace(/\.[^/.]+$/, "");
};

export async function getFileRows(file: File) {
  const text = await readFileAsText(file);

  const rows = cleanFileContent(text);

  if (rows.length === 0) {
    console.error("An empty file was dropped");
  }

  return rows;
}

export async function getUniqueFileRows(file: File): Promise<string[]> {
  const rows = await getFileRows(file);

  // Take care of duplicate rows
  return Array.from(new Set(rows));
}
