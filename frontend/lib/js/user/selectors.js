// @flow

import type { StateType } from "../app/reducers";
import type { Permission } from "../api/types";

type ContextT = {
  datasetId?: string
};

export function selectPermissions(state: StateType): ?(Permission[]) {
  return !!state.user.me && !!state.user.me.permissions
    ? state.user.me.permissions
    : null;
}

const permissionHasDataset = (permission: Permission) =>
  permission.domains.includes("datasets") || permission.domains.includes("*");
const permissionFitsTarget = (permission: Permission, datasetId: string) =>
  permission.targets.includes(datasetId) || permission.targets.includes("*");

function canDoNothing(permissions: Permission[], datasetId: string) {
  return permissions.every(permission => {
    const hasDataset = permissionHasDataset(permission);
    const fitsTarget = permissionFitsTarget(permission, datasetId);

    return !hasDataset || !fitsTarget;
  });
}

function canDoEverything(permissions: Permission[], datasetId: string) {
  return permissions.some(permission => {
    const hasDataset = permissionHasDataset(permission);
    const fitsTarget = permissionFitsTarget(permission, datasetId);

    const hasAllAbilities = permission.abilities.includes("*");

    return hasAllAbilities && fitsTarget && hasDataset;
  });
}

function canDo(
  state: StateType,
  canDoWithPermissions: (permissions: Permission[]) => boolean,
  context?: ContextT
) {
  const permissions = selectPermissions(state);

  if (!permissions) return false;

  const { selectedDatasetId } = state.datasets;
  const dataset: ?string =
    context && context.datasetId ? context.datasetId : selectedDatasetId;

  if (!dataset) {
    return false;
  }

  if (canDoNothing(permissions, dataset)) return false;
  if (canDoEverything(permissions, dataset)) return true;

  return canDoWithPermissions(permissions);
}

export function canDownloadResult(state: StateType, datasetId?: string) {
  return canDo(
    state,
    permissions => {
      return permissions.some(permission =>
        permission.abilities.includes("download")
      );
    },
    { datasetId }
  );
}

export function canUploadResult(state: StateType) {
  return canDo(state, permissions => {
    return permissions.some(permission =>
      permission.abilities.includes("preserve_id")
    );
  });
}
