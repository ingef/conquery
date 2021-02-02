import thunk from "redux-thunk";
import multi from "redux-multi";

export default function () {
  return [thunk, multi];
}
