export const exists = <T>(value: T | undefined | null): value is T =>
  // we're intentionally checking for null or undefined here
  // eslint-disable-next-line eqeqeq
  value != null;
