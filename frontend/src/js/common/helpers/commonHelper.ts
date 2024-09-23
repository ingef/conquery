export const concat = (arr: []) => arr.reduce((a, b) => a.concat(b), []);

export const compose = <R>(fn1: (a: R) => R, ...fns: Array<(a: R) => R>) =>
  fns.reduce((prevFn, nextFn) => (value) => prevFn(nextFn(value)), fn1);

export const isEmpty = (variable: unknown) => {
  return (
    typeof variable === "undefined" ||
    variable === null ||
    variable === "" ||
    (variable instanceof Array && variable.length === 0) ||
    (variable.constructor === Object && Object.keys(variable).length === 0)
  );
};

export const numberToThreeDigitArray = (number: number) => {
  // Input: 10       Output: [10]
  // Input: 1234     Output: [1, 234]
  // Input: 24521280 Output: [23, 521, 280]
  //
  // Taken from:
  // http://stackoverflow.com/questions/2901102/
  // how-to-print-a-number-with-commas-as-thousands-separators-in-javascript
  return number
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, " ")
    .split(" ");
};

export const toUpperCaseUnderscore = (str: string) => {
  if (str.toUpperCase() === str) return str;

  return str
    .replace(/[A-Z]/g, (upperCaseChar) => "_" + upperCaseChar.toLowerCase())
    .toUpperCase();
};
