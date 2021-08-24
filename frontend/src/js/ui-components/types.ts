//
// TODO: Get rid of redux-form, then refactor this. See README for more info.
//
// Residue struct of redux-form, which doesn't have great typescript types
// Those are passed to a redux form field
export interface InputProps<T> {
  input: {
    value: T;
    onChange: (value: T) => void;
    defaultValue?: T;
  };
}
