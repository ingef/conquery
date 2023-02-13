// ----------------------------------
// PRODUCTION SERVER (see Dockerfile)
// ----------------------------------

const express = require("express");
const app = express();
const compression = require("compression");
const helmet = require("helmet");
const cors = require("cors");
const path = require("path");
const rateLimit = require("express-rate-limit");

const PORT = process.env.PORT || 8000;
// Maximum requests per minute
const requestsPerMinute = process.env.REQUESTS_PER_MINUTE || 1000;

app.use(
  helmet({
    contentSecurityPolicy: false,
  }),
);
app.disable("x-powered-by");
app.use(compression());
app.use(cors());

// set up rate limiter: default maximum of 1000 requests per minute
app.use(
  rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: requestsPerMinute,
  }),
);

app.use(express.static(path.resolve(__dirname, "build")));
app.get("*", (req, res) =>
  res.sendFile(path.join(__dirname, "build", "index.html")),
);
app.use((req, res, next) => {
  if (req.accepts(`html`)) {
    res.status(404);
  } else {
    next();
  }
});

app.listen(PORT, () => console.log(`Node server listening on port: ${PORT}`));
