// Using a conditional require, so that dependencies that are imported
// only in devMiddleware, are not bundled into the production build.
if (process.env.NODE_ENV === "production")
  module.exports = require("./prodMiddleware.js");
else module.exports = require("./devMiddleware.js");
