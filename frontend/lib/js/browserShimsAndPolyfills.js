// Remove fetch from window object so polyfill is used
if (window.navigator.userAgent.indexOf("Edge") > -1) window.fetch = undefined;

require("es6-promise").polyfill();

if (!("classList" in SVGElement.prototype)) {
  Object.defineProperty(SVGElement.prototype, "classList", {
    get() {
      return {
        contains: className => {
          return this.className.baseVal.split(" ").indexOf(className) !== -1;
        }
      };
    }
  });
}
