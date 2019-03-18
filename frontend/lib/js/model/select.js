// flow

export function objectHasSelectedSelects(obj) {
  return (
    obj &&
    obj.selects &&
    obj.selects.some(
      select =>
        (select.selected && !select.default) ||
        (!select.selected && !!select.default)
    )
  );
}
