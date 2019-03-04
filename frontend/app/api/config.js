const version = require("../../package.json").version;
const config = require("./config.json");

config.version = version;

export default config;
