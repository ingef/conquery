import { toUpperCaseUnderscore } from '../common/helpers';
import { createActionTypes }     from './actionTypes';

export default function createQueryNodeModalReducer(type) {
  const initialState = {
    andIdx: null,
    orIdx: null
  };

  const uppercasedType = toUpperCaseUnderscore(type);
  const actionTypes = createActionTypes(uppercasedType);

  return (state = initialState, action) => {
    switch (action.type) {
      case actionTypes.SET_NODE:
        return {
          ...state,
          andIdx: action.payload.andIdx,
          orIdx: action.payload.orIdx
        };
      case actionTypes.CLEAR_NODE:
        return {
          ...state,
          andIdx: null,
          orIdx: null
        };
      default:
        return state;
    }
  };
}
