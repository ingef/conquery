const path = require('path');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');

const conqueryConfig = require('../webpack.config.js');

module.exports = {
  ...conqueryConfig,
  devtool: 'source-map',
  entry: {
    main: [
      'webpack-hot-middleware/client?reload=true',
      path.join(__dirname, 'src/js/main.js')
    ]
  },
  output: {
    path: path.join(__dirname, 'dist/'),
    filename: '[name].js',
    publicPath: '/'
  },
  plugins: [
    ...conqueryConfig.plugins,
    new HtmlWebpackPlugin({
      template: path.join(__dirname, 'src/index.tpl.html'),
      inject: 'body',
      filename: 'index.html'
    }),
    new webpack.HotModuleReplacementPlugin(),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('development')
    }),
  ],
  module: {
    ...conqueryConfig.module,
    rules: [
      ...conqueryConfig.module.rules
        .filter(rule => rule.loader !== 'file-loader'),
      // Set a file loader that has no output path set
      {
        test: /\.(ttf|eot|svg|png|jpg|woff(2)?)(\?.*$|$)/,
        loader: "file-loader?name=[name].[ext]"
      },
      {
        test: /\.sass$/,
        loader: "style-loader!css-loader!postcss-loader!sass-loader?indentedSyntax"
      },
    ]
  },
  node: {
    fs: "empty"
  }
};
