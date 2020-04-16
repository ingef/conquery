// import { createLogger }  from 'redux-logger';
import prodMiddleware from "./prodMiddleware";

export default function(browserHistory) {
  // const reduxLogger = createLogger({
  //   diff: false,
  // });

  return [
    // Dev middlware uses all of production, plus some extra
    ...prodMiddleware(browserHistory)
    // reduxLogger,
  ];
}
