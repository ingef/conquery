import { StateT } from "app-types";
import { useSelector } from "react-redux";

import { DatasetIdT } from "../api/types";

export const useDatasetId = () => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );

  return datasetId;
};
