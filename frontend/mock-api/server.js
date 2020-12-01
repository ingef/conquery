// -----------
// EXPRESS SETUP
// -----------
const path = require("path");
const express = require("express");
const bodyParser = require("body-parser");
const cors = require("cors");
const mountApi = require(".");

const isDeveloping = process.env.NODE_ENV !== "production";
const port = process.env.PORT || 8001;
const app = express();
const lang = process.env.APP_LANG || "en";

app.use(cors());
// body parser must be set up before routes are attached
app.use(bodyParser.json());

mountApi(app, port);

console.log(`MODE: ${isDeveloping ? "dev" : "production"}`);

if (!isDeveloping) {
  app.use("/app/static", express.static(path.join(__dirname, "../build")));
}

app.listen(port, "0.0.0.0", function onStart(err) {
  if (err) {
    console.log(err);
  }

  console.info("==> 🌎 Listening on port %s.", port);
});
