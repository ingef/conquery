const path = require('path');
const webpack = require('webpack');

module.exports = {
  entry: {
    main: path.join(__dirname, 'lib/js/conquery.js')
  },
  output: {
    path: path.join(__dirname, 'dist/'),
    filename: 'conquery.js',
    library: 'conquery',
    libraryTarget: 'umd',
    umdNamedDefine: true
  },
  plugins: [
    new webpack.NoEmitOnErrorsPlugin()
  ],
  module: {
    rules: [{
      test: /\.js$/,
      exclude: path.join(__dirname, 'node_modules/'),
      loader: 'babel-loader'
    }, {
      test: /\.json$/,
      loader: 'json-loader'
    }, {
      test: /\.yml$/,
      loader: 'json-loader!yaml-loader'
    }, {
      test: /\.(ttf|eot|svg|png|jpg|woff(2)?)(\?.*$|$)/,
      loader: "file-loader",
      options: {
        name: '[name].[ext]',
        outputPath: 'files/'
      }
    }]
  }
};
