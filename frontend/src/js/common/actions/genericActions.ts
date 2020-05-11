export interface ActionT {
  type: string;
  error?: boolean;
  payload?: {
    [k: string]: any;
  };
}

interface ErrorObject {
  message: string;
  status: string;
}

export const defaultError = (
  type: string,
  error: ErrorObject,
  payload?: Object
): ActionT => ({
  type,
  error: true,
  payload: {
    message: error.message,
    status: error.status,
    ...payload,
  },
});

export const defaultSuccess = (
  type: string,
  results: any,
  payload?: Object
): ActionT => ({
  type,
  payload: {
    data: results,
    receivedAt: Date.now(),
    ...payload,
  },
});
