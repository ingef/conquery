export function selectPermissions(state) {
  return !!state.user.me && !!state.user.me.permissions
    ? state.user.me.permissions
    : null;
}

export function canDownloadResult(state) {
  const permissions = selectPermissions(state);

  if (!permissions) return false;

  const permission = permissions.find(
    p => p.domains.includes("datasets") && p.abilities.includes("download")
  );

  const { selectedDatasetId } = state.datasets;

  return (
    !!permission &&
    (permission.targets.includes("*") ||
      // TODO: This makes not that much sense yet, because
      //       the dataset shouldn't be the selected one, but the one
      //       which relates to the result
      permission.targets.includes(selectedDatasetId))
  );
}
