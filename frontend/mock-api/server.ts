// -----------
// EXPRESS SETUP
// -----------
import bodyParser from "body-parser";
import cors from "cors";
import express from "express";

import mountApi from "./mockApi.js";

const port = process.env.PORT || 8001;
const app = express();

app.use(cors());
// body parser must be set up before routes are attached
app.use(
  bodyParser.json({
    limit: "100mb",
  }),
);

mountApi(app);

app.listen(Number(port), "0.0.0.0", () => {
  console.info("==> 🌎 Listening on port %s.", port);
});
