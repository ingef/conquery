import { useSelector } from "react-redux";

import type { DatasetT } from "../api/types";
import type { StateT } from "../app/reducers";

export const useDatasetId = () => {
  const datasetId = useSelector<StateT, DatasetT["id"] | null>(
    (state) => state.datasets.selectedDatasetId,
  );

  return datasetId;
};
