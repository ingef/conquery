const path = require('path');
const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');

const conqueryConfig = require('./webpack.common.config.js');

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
    new webpack.HotModuleReplacementPlugin(),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('development')
    }),
  ],
  module: {
    ...conqueryConfig.module,
    rules: [
      ...conqueryConfig.module.rules,
      {
        test: /\.sass$/,
        loaders: [
          'style-loader',
          'css-loader',
          'postcss-loader',
          'resolve-url-loader',
          {
            loader: 'sass-loader',
            options: {
              indentedSyntax: true,
              sourceMap: true, // Necessary for resolve-url
              includePaths: [path.join(__dirname, 'node_modules/conquery/dist/styles')]
            }
          }
        ]
      },
    ]
  },
  node: {
    fs: "empty"
  }
};
