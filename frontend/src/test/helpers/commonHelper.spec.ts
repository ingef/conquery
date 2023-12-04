import { isEmpty } from "../../js/common/helpers/commonHelper";

describe("helpers", () => {
  describe("commonHelper", () => {
    describe("isEmpty", () => {
      it("works with null / undefined", () => {
        expect(isEmpty(null)).toBe(true);
        expect(isEmpty(undefined)).toBe(true);
      });

      it("works with strings", () => {
        expect(isEmpty("")).toBe(true);
        expect(isEmpty("ab")).toBe(false);
        expect(isEmpty("awef awef")).toBe(false);
      });

      it("works with numbers", () => {
        expect(isEmpty(-1)).toBe(false);
        expect(isEmpty(0)).toBe(false);
        expect(isEmpty(1)).toBe(false);
      });

      it("works with an array", () => {
        expect(isEmpty([])).toBe(true);
        expect(isEmpty([0])).toBe(false);
        expect(isEmpty([0, 1])).toBe(false);
      });

      it("works with an empty object", () => {
        expect(isEmpty({})).toBe(true);
        expect(isEmpty({ a: "abc" })).toBe(false);
        expect(isEmpty({ yellow: "black", black: "yellow" })).toBe(false);
      });
    });
  });
});
