// @flow

export const readFileAsText = (file: File) => new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = (evt) => resolve(evt.target.result);
    reader.onerror = (err) => reject(err);

    reader.readAsText(file);
});

/**
 * Split by \n, trim rows and filter empty rows
 * @param fileContent
 */
export const cleanFileContent = (fileContent: string) => {
    return fileContent.split('\n')
      .map(row => row.trim())
      .filter(row => row.length > 0);
};

export const checkFileType = (file: File, type?: string) => {
    return file.type === type || "text/plain";
};
