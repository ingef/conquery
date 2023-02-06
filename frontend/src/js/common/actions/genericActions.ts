export interface ErrorObject {
  message?: string;
  status?: string;
}

export const errorPayload = <T extends Object>(
  error: ErrorObject,
  context: T,
): T & ErrorObject => ({
  message: error.message,
  status: error.status,
  ...context,
});

export const successPayload = <T, Context>(
  data: T,
  context: Context,
): { data: T } & Context => ({
  data,
  ...context,
});
