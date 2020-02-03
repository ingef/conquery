export function selectPermissions(state) {
  return !!state.user.me && !!state.user.me.permissions
    ? state.user.me.permissions
    : null;
}

export function canDownloadResult(state, datasetId?: string) {
  const permissions = selectPermissions(state);

  if (!permissions) return false;

  const { selectedDatasetId } = state.datasets;

  // TODO: Fallback to selectedDatasetId probably doesn't make that much sense,
  //       because the dataset to use should probably always relates to the result
  //       or entity, that is being checked here.
  //
  const dataset = datasetId || selectedDatasetId;

  const permission = permissions.find(
    p =>
      p.domains.includes("datasets") &&
      p.abilities.includes("download") &&
      (p.targets.includes("*") || p.targets.includes(dataset))
  );

  return !!permission;
}
