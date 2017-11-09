import dirtyChai from 'dirty-chai';
import chai from 'chai';

export default function setupChai() {
  [
    dirtyChai,
    // new plugins here ...
  ].map(chai.use.bind(chai)); // iterate over the plugins and call chai.use() with each
};
