import { combineReducers } from "redux";

import createQueryRunnerReducer, {
  QueryRunnerStateT,
} from "../../query-runner/reducer";

export interface UploadQueryResultsStateT {
  queryRunner: QueryRunnerStateT;
}

const queryRunner = createQueryRunnerReducer("external");

export default combineReducers({
  queryRunner,
});
