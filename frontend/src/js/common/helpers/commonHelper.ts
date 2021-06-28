export const concat = (arr: []) => arr.reduce((a, b) => a.concat(b), []);

export const compose = <R>(fn1: (a: R) => R, ...fns: Array<(a: R) => R>) =>
  fns.reduce((prevFn, nextFn) => (value) => prevFn(nextFn(value)), fn1);

export const objectWithoutKey = (key: string) => (obj: Object) => {
  if (!obj.hasOwnProperty(key)) return obj;

  const { [key]: deleted, ...rest } = obj;

  return rest;
};

export const isEmpty = (variable: any) => {
  return (
    typeof variable === "undefined" ||
    variable === null ||
    variable === "" ||
    (variable instanceof Array && variable.length === 0) ||
    (variable.constructor === Object && Object.keys(variable).length === 0)
  );
};

export const isEmptyObject = (variable: any) => {
  if (!variable) return false;

  return (
    variable.constructor === Object &&
    (Object.keys(variable).length === 0 ||
      (Object.keys(variable).length > 0 &&
        Object.keys(variable).every((k) => typeof variable[k] === "undefined")))
  );
};

export const includes = (array: any[], element: any) => {
  return array.indexOf(element) !== -1;
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

export const capitalize = (str: string) =>
  str.charAt(0).toUpperCase() + str.slice(1);

export const toUpperCaseUnderscore = (str: string) => {
  if (str.toUpperCase() === str) return str;

  return str
    .replace(/[A-Z]/g, (upperCaseChar) => "_" + upperCaseChar.toLowerCase())
    .toUpperCase();
};

export const isObject = (item: any) =>
  item && typeof item === "object" && !Array.isArray(item);

/**
 * pass in your object structure as array elements
 * user = {
 *  name: 'Peter Lustig',
 *  address: {
 *    street: 'Am Loewenzahn 1'
 *  }
 * }
 * e.g.: const name = getNestedObject(user, ['address', 'street']);
 */
export const getNestedObject = (nestedObj, pathArr) => {
  return pathArr.reduce(
    (obj, key) => (obj && obj[key] !== "undefined" ? obj[key] : undefined),
    nestedObj,
  );
};
