// Before, we've been using stage-1. @babel/preset-stage-1 was deprecated for good reason.
// Now we're still using some early-stage proposals
// TODO: Refactor the code and get rid of them

module.exports = {
  "env": {
    "development": {
      "presets": [
        ["@babel/preset-env", {"modules": false}],
        "@babel/preset-react",
        "@babel/preset-flow"
      ],
      "plugins": [
        "react-hot-loader/babel",
        "@babel/plugin-proposal-export-namespace-from",
        [ "@babel/plugin-proposal-class-properties", {loose: true}],
        "@babel/plugin-proposal-export-default-from"
      ]
    },
    "test": {
      "presets": ["@babel/preset-env", "@babel/preset-react", "@babel/preset-flow" ],
      "plugins": [
        "@babel/plugin-proposal-export-namespace-from",
        [ "@babel/plugin-proposal-class-properties", {loose: true}],
        "@babel/plugin-proposal-export-default-from"
      ]
    },
    "production": {
      "presets": ["@babel/preset-env", "@babel/preset-react", "@babel/preset-flow" ],
      "plugins": [
        "@babel/plugin-proposal-export-namespace-from",
        [ "@babel/plugin-proposal-class-properties", {loose: true}],
        "@babel/plugin-proposal-export-default-from"
      ]
    }
  }
}
