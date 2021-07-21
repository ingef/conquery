// -----------
// EXPRESS SETUP
// -----------
const path = require("path");
const express = require("express");
const bodyParser = require("body-parser");
const cors = require("cors");
const mountApi = require(".");

const port = process.env.PORT || 8001;
const app = express();

app.use(cors());
// body parser must be set up before routes are attached
app.use(bodyParser.json());

mountApi(app, port);

app.listen(port, "0.0.0.0", function onStart(err) {
  if (err) {
    console.log(err);
  }

  console.info("==> 🌎 Listening on port %s.", port);
});
