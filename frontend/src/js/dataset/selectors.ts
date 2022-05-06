import { useSelector } from "react-redux";

import { DatasetIdT } from "../api/types";
import type { StateT } from "../app/reducers";

export const useDatasetId = () => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );

  return datasetId;
};
