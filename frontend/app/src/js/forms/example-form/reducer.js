import {combineReducers} from "redux";

import {createQueryNodeModalReducer} from "../../../../../lib/js/query-node-modal";

const exampleReducer1 = createQueryNodeModalReducer('example1');
const exampleReducer2 = createQueryNodeModalReducer('example2');

export default combineReducers({
  example1: exampleReducer1,
  example2: exampleReducer2
});
