export const exists = <T>(value: T | undefined | null): value is T =>
  value != null;
