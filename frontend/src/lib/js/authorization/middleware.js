// @flow

import { push } from "react-router-redux";

export function createUnauthorizedErrorMiddleware() {
  return (store: Object) => (next: Function) => (action: any) => {
    if (action.payload && action.payload.status === 401)
      store.dispatch(push("/unauthorized"));

    return next(action);
  };
}
