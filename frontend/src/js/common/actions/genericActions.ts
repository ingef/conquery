interface ActionT {
  type: string;
  error?: boolean;
  payload?: {
    [k: string]: any;
  };
}

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

export const defaultError = (
  type: string,
  error: ErrorObject,
  context?: Record<string, any>,
): ActionT => ({
  type,
  error: true,
  payload: {
    message: error.message,
    status: error.status,
    ...context,
  },
});

export const successPayload = <T, Context>(
  data: T,
  context: Context,
): { data: T } & Context => ({
  data,
  ...context,
});

export const defaultSuccess = (
  type: string,
  results: any,
  context?: Record<string, any>,
): ActionT => ({
  type,
  payload: {
    data: results,
    receivedAt: Date.now(),
    ...context,
  },
});
