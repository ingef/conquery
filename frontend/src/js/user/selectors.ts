import { useSelector } from "react-redux";

import type { DatasetT, GetMeResponseT, PermissionsT } from "../api/types";
import type { StateT } from "../app/reducers";

interface ContextT {
  datasetId?: string;
}

export function selectPermissions(
  state: StateT,
): Record<DatasetT["id"], PermissionsT> | null {
  return !!state.user.me && !!state.user.me.datasetAbilities
    ? state.user.me.datasetAbilities
    : null;
}

function canDoNothing(
  permissions: Record<DatasetT["id"], PermissionsT>,
  datasetId: string,
) {
  return !permissions[datasetId];
}

function canDo(
  state: StateT,
  canDoWithPermissions: (
    permissions: Record<DatasetT["id"], PermissionsT>,
    datasetId: string,
  ) => boolean,
  context?: ContextT,
) {
  const permissions = selectPermissions(state);

  if (!permissions) return false;

  const { selectedDatasetId } = state.datasets;
  const datasetId: string | null =
    context && context.datasetId ? context.datasetId : selectedDatasetId;

  if (!datasetId) {
    return false;
  }

  if (canDoNothing(permissions, datasetId)) return false;

  return canDoWithPermissions(permissions, datasetId);
}

// Example of another possible permission, including a "context"
// export function canDownloadResult(state: StateT, datasetId?: string) {
//   return canDo(
//     state,
//     (permissions, finalDatasetId) => {
//       return permissions...
//     },
//     { datasetId }
//   );
// }

export function canViewEntityPreview(state: StateT) {
  return canDo(state, (permissions, datasetId) => {
    return permissions[datasetId].canViewEntityPreview === true;
  });
}

export function canViewQueryPreview(state: StateT) {
  return canDo(state, (permissions, datasetId) => {
    return permissions[datasetId].canViewQueryPreview === true;
  });
}

export function canUploadResult(state: StateT) {
  return canDo(state, (permissions, datasetId) => {
    return permissions[datasetId].canUpload === true;
  });
}

export function useHideLogoutButton() {
  const me = useSelector<StateT, GetMeResponseT | null>(
    (state) => state.user.me,
  );

  return !!me && (me.hideLogoutButton === undefined || !!me.hideLogoutButton);
}
