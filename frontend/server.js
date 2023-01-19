// ----------------------------------
// PRODUCTION SERVER (see Dockerfile)
// ----------------------------------

const express = require("express");
const app = express();
const compression = require("compression");
const helmet = require("helmet");
const cors = require("cors");
const path = require("path");

const PORT = process.env.PORT || 8000;

app.use(
  helmet({
    contentSecurityPolicy: false,
  })
);
app.disable("x-powered-by");
app.use(compression());
app.use(cors());

app.use(express.static(path.resolve(__dirname, "build")));
app.get("*", (req, res) =>
  res.sendFile(path.join(__dirname, "build", "index.html"))
);
app.use((req, res, next) => {
  if (req.accepts(`html`)) {
    res.status(404);
  } else {
    next();
  }
});

app.listen(PORT, () => console.log(`Node server listening on port: ${PORT}`));
