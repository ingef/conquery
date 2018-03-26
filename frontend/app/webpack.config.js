const path = require('path');
const webpack = require('webpack');
const ProgressBarPlugin = require('progress-bar-webpack-plugin');

const commonConfig = require('./webpack.common.config.js');

module.exports = ['en', 'de'].map(lang => ({
  ...commonConfig[lang],
  name: lang,
  devtool: 'eval-source-map',
  entry: {
    main: [
      'babel-polyfill',
      'webpack-hot-middleware/client?reload=true',
      path.join(__dirname, `src/js/main.${lang}.js`)
    ]
  },
  output: {
    path: path.join(__dirname, 'dist/'),
    filename: `[name].${lang}.js`,
    publicPath: '/'
  },
  plugins: [
    ...commonConfig[lang].plugins,
    new ProgressBarPlugin(),
    new webpack.HotModuleReplacementPlugin(),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('development')
    }),
  ],
  module: {
    ...commonConfig[lang].module,
    rules: [
      ...commonConfig[lang].module.rules,
      {
        test: /\.sass$/,
        loaders: [
          'style-loader',
          'css-loader',
          { loader: 'postcss-loader', options: { sourceMap: true } },
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
    fs: 'empty'
  }
}));
