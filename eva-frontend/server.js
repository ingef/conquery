// -----------
// EXPRESS SETUP
// -----------
const path = require("path");
const express = require("express");

const isDeveloping = process.env.NODE_ENV !== "production";
const port = process.env.PORT || 8000;
const app = express();

if (isDeveloping) {
  const webpack = require("webpack");
  const webpackMiddleware = require("webpack-dev-middleware");
  const webpackHotMiddleware = require("webpack-hot-middleware");
  const config = require("./webpack.config.js");
  const simulateDevApi = require("./api");
  const bodyParser = require("body-parser");

  const compiler = webpack(config);
  const middleware = webpackMiddleware(compiler, {
    publicPath: config.output.publicPath,
    contentBase: "src",
    stats: {
      colors: true,
      hash: false,
      timings: true,
      chunks: false,
      chunkModules: false,
      modules: false
    }
  });

  // body parser must be set up before routes are attached
  app.use(bodyParser.json());
  simulateDevApi(app, port);

  app.use(middleware);
  app.use(webpackHotMiddleware(compiler));
  app.get("*", function response(req, res) {
    res.write(
      middleware.fileSystem.readFileSync(
        path.join(__dirname, "dist/index.html")
      )
    );
    res.end();
  });
} else {
  app.use(express.static(path.join(__dirname, "dist")));
  app.use('/app/static', express.static(path.join(__dirname, "dist")));
}

app.listen(port, "0.0.0.0", function onStart(err) {
  if (err) {
    console.log(err);
  }

  console.info("==> 🌎 Listening on port %s.", port);
});
