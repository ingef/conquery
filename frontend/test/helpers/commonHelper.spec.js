import { expect } from 'chai';
import {
  isEmpty,
  isEmptyObject,
  stripObject,
} from '../../lib/js/common/helpers';

describe('helpers', () => {
  describe('commonHelper', () => {
    describe('isEmpty', () => {
      it('works with null / undefined', () => {
        expect(isEmpty(null)).to.be.true();
        expect(isEmpty(undefined)).to.be.true();
      });

      it('works with strings', () => {
        expect(isEmpty("")).to.be.true();
        expect(isEmpty("ab")).to.be.false();
        expect(isEmpty("awef awef")).to.be.false();
      });

      it('works with numbers', () => {
        expect(isEmpty(-1)).to.be.false();
        expect(isEmpty(0)).to.be.false();
        expect(isEmpty(1)).to.be.false();
      });

      it('works with an array', () => {
        expect(isEmpty([])).to.be.true();
        expect(isEmpty([0])).to.be.false();
        expect(isEmpty([0, 1])).to.be.false();
      });

      it('works with an empty object', () => {
        expect(isEmpty({})).to.be.true();
        expect(isEmpty({a: 'abc'})).to.be.false();
        expect(isEmpty({yellow: "black", black: "yellow"})).to.be.false();
      });
    });

    describe('isEmptyObject', () => {
      it('works with unempty objects', () => {
        expect(isEmptyObject(null)).to.be.false();
        expect(isEmptyObject({ foo: undefined, bar: false })).to.be.false();
        expect(isEmptyObject({ foo: "yo" })).to.be.false();
      });

      it('works with empty objects', () => {
        expect(isEmptyObject({})).to.be.true();
        expect(isEmptyObject({ a: undefined })).to.be.true();
        expect(isEmptyObject({ foo: undefined, bar: undefined })).to.be.true();
      });
    });

    describe('stripObject', () => {
      it('works with empty object', () => {
        expect(stripObject({})).to.deep.equal({});
      });

      it('works with non-empty object', () => {
        expect(stripObject({ a: 1, b: 2 })).to.deep.equal({a: 1, b: 2});
        // expect(stripObject({ a: undefined, b: 2 })).to.deep.equal({a: undefined, b: 2});
        // expect(stripObject({ a: {}, b: 2 })).to.deep.equal({a: undefined, b: 2});
        // expect(stripObject({ a: "", b: 2 })).to.deep.equal({a: undefined, b: 2});
      });
    });
  });
});
