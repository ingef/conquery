window.datasetId = null;

export function getDatasetId() {
  return window.datasetId;
}

export function setDatasetId(id: string | null) {
  window.datasetId = id;
}
