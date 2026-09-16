/**
 * Backend configs describe a number's shape as a regex, e.g. `^(?!-)\d*$`.
 * Probing it with sample inputs turns that into NumberField constraints.
 */
export const numberPatternConstraints = (
  pattern?: string | null,
): { minValue?: number; maximumFractionDigits?: number } => {
  if (!pattern) return {};

  const regex = new RegExp(pattern);

  return {
    ...(regex.test("-1") ? {} : { minValue: 0 }),
    ...(regex.test("1,5") ? {} : { maximumFractionDigits: 0 }),
  };
};
