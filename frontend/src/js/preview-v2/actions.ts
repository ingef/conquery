import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";
import { useGetResult } from "../api/api";
import { StateT } from "../app/reducers";
import { ErrorObject } from "../common/actions/genericActions";
import { PreviewStateT } from "./reducer";

export type PreviewActions = ActionType<
  typeof loadPreview | typeof closePreview | typeof openPreview
>;

interface PreviewData {
  arrowFile: File;
  queryId: number;
}

export const loadPreview = createAsyncAction(
  "preview/LOAD_START",
  "preview/LOAD_SUCCESS",
  "preview/LOAD_ERROR",
)<void, PreviewData, ErrorObject>();

export const openPreview = createAction("preview/OPEN")();
export const closePreview = createAction("preview/CLOSE")();

export function useLoadPreviewData() {
  const dispatch = useDispatch();
  const getResult = useGetResult();

  const { dataLoadedForQueryId, arrowFile } = useSelector<
    StateT,
    PreviewStateT
  >((state) => state.preview);
  const currentPreviewData: PreviewData | null =
    dataLoadedForQueryId && arrowFile
      ? {
          queryId: dataLoadedForQueryId,
          arrowFile,
        }
      : null;

  return async (
    queryId: number,
    { noLoading }: { noLoading: boolean } = { noLoading: false },
  ): Promise<PreviewData | null> => {
    if (currentPreviewData && dataLoadedForQueryId === queryId) {
      return currentPreviewData;
    }

    if (!noLoading) {
      dispatch(loadPreview.request());
    }

    const resultData = await getResult(queryId);
    const payload = {
      arrowFile: resultData,
      queryId,
    };

    dispatch(loadPreview.success(payload));
    return payload;
  };
}
