// import { createLogger }  from 'redux-logger';
import prodMiddleware from "./prodMiddleware";

export default function () {
  // const reduxLogger = createLogger({
  //   diff: false,
  // });

  return [
    // Dev middlware uses all of production, plus some extra
    ...prodMiddleware(),
    // reduxLogger,
  ];
}
