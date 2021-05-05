import multi from "redux-multi";
import thunk from "redux-thunk";

export default function () {
  return [thunk, multi];
}
