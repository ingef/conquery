const path = require('path');
const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');

const commonConfig = require('./webpack.common.config.js');

module.exports = {
  ...commonConfig,
  devtool: 'source-map',
  entry: {
    main: [
      'react-hot-loader/patch',
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
    ...commonConfig.plugins,
    new webpack.HotModuleReplacementPlugin(),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('development')
    }),
  ],
  module: {
    ...commonConfig.module,
    rules: [
      ...commonConfig.module.rules,
      {
        test: /\.sass$/,
        loader: "style-loader!css-loader!postcss-loader!sass-loader?indentedSyntax"
      }
    ]
  },
  node: {
    fs: "empty"
  }
};
