// Before, we've been using stage-1. @babel/preset-stage-1 was deprecated for good reason.
// Now we're still using some early-stage proposals
// TODO: Refactor the code and get rid of them

module.exports = {
  presets: ["react-app"],
  plugins: [
    "@babel/plugin-proposal-export-default-from",
    "@babel/plugin-proposal-export-namespace-from",
    "emotion"
  ]
};
