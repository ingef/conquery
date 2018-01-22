// -----------
// EXPRESS SETUP
// -----------
var path         = require('path');
var express      = require('express');
var bodyParser   = require('body-parser');
var mountApi     = require('./api');

var isDeveloping = process.env.NODE_ENV !== 'production';
var port         = isDeveloping ? 8000 : process.env.PORT;
var app          = express();

// body parser must be set up before routes are attached
app.use(bodyParser.json());
mountApi(app, port);

if (isDeveloping) {
  var webpack              = require('webpack');
  var webpackMiddleware    = require('webpack-dev-middleware');
  var webpackHotMiddleware = require('webpack-hot-middleware');
  var config               = require('./webpack.config.js');

  var compiler   = webpack(config);
  var middleware = webpackMiddleware(compiler, {
    publicPath: config.output.publicPath,
    contentBase: 'src',
    stats: {
      colors: true,
      hash: false,
      timings: true,
      chunks: false,
      chunkModules: false,
      modules: false
    }
  });

  app.use(middleware);
  app.use(webpackHotMiddleware(compiler));
  app.get('*', function response(req, res) {
    res.write(middleware.fileSystem.readFileSync(path.join(__dirname, 'dist/index.html')));
    res.end();
  });
} else {
  app.use(express.static(__dirname + '/dist'));
  app.get('*', function response(req, res) {
    res.sendFile(path.join(__dirname, 'dist/index.html'));
  });
}


app.listen(port, '0.0.0.0', function onStart(err) {
  if (err) {
    console.log(err);
  }

  console.info('==> 🌎 Listening on port %s.', port);
});
