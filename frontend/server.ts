// ----------------------------------
// PRODUCTION SERVER (see Dockerfile)
// ----------------------------------
import compression from "compression";
import cors from "cors";
import express from "express";
import rateLimit from "express-rate-limit";
import helmet from "helmet";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const BUILD_FOLDER = "dist";

const PORT = process.env.PORT || 8000;
// Maximum requests per minute
const requestsPerMinute = process.env.REQUESTS_PER_MINUTE || 1000;

const app = express();

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
    max: Number(requestsPerMinute),
  }),
);

app.use(express.static(path.resolve(__dirname, BUILD_FOLDER)));
app.get("*", (_, res) =>
  res.sendFile(path.join(__dirname, BUILD_FOLDER, "index.html")),
);
app.use((req, res, next) => {
  if (req.accepts(`html`)) {
    res.status(404);
  } else {
    next();
  }
});

app.listen(PORT, () => console.log(`Node server listening on port: ${PORT}`));
