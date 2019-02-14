import {
  isEmpty,
  isEmptyObject,
  stripObject,
} from '../../lib/js/common/helpers';

describe('helpers', () => {
  describe('commonHelper', () => {
    describe('isEmpty', () => {
      it('works with null / undefined', () => {
        expect(isEmpty(null)).toBe(true);
        expect(isEmpty(undefined)).toBe(true);
      });

      it('works with strings', () => {
        expect(isEmpty("")).toBe(true);
        expect(isEmpty("ab")).toBe(false);
        expect(isEmpty("awef awef")).toBe(false);
      });

      it('works with numbers', () => {
        expect(isEmpty(-1)).toBe(false);
        expect(isEmpty(0)).toBe(false);
        expect(isEmpty(1)).toBe(false);
      });

      it('works with an array', () => {
        expect(isEmpty([])).toBe(true);
        expect(isEmpty([0])).toBe(false);
        expect(isEmpty([0, 1])).toBe(false);
      });

      it('works with an empty object', () => {
        expect(isEmpty({})).toBe(true);
        expect(isEmpty({a: 'abc'})).toBe(false);
        expect(isEmpty({yellow: "black", black: "yellow"})).toBe(false);
      });
    });

    describe('isEmptyObject', () => {
      it('works with unempty objects', () => {
        expect(isEmptyObject(null)).toBe(false);
        expect(isEmptyObject({ foo: undefined, bar: false })).toBe(false);
        expect(isEmptyObject({ foo: "yo" })).toBe(false);
      });

      it('works with empty objects', () => {
        expect(isEmptyObject({})).toBe(true);
        expect(isEmptyObject({ a: undefined })).toBe(true);
        expect(isEmptyObject({ foo: undefined, bar: undefined })).toBe(true);
      });
    });

    describe('stripObject', () => {
      it('works with empty object', () => {
        expect(stripObject({})).toEqual({});
      });

      it('works with non-empty object', () => {
        expect(stripObject({ a: 1, b: 2 })).toEqual({a: 1, b: 2});
        // expect(stripObject({ a: undefined, b: 2 })).to.deep.equal({a: undefined, b: 2});
        // expect(stripObject({ a: {}, b: 2 })).to.deep.equal({a: undefined, b: 2});
        // expect(stripObject({ a: "", b: 2 })).to.deep.equal({a: undefined, b: 2});
      });
    });
  });
});
