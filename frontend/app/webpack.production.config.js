const path = require('path');
const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;

const commonConfig = require('./webpack.common.config.js');

module.exports = ['en', 'de'].map(lang => ({
  ...commonConfig[lang],
  name: lang,
  entry: {
    main: [
      'babel-polyfill',
      path.join(__dirname, `src/js/main.${lang}.js`)
    ]
  },
  output: {
    path: path.join(__dirname, '/dist/'),
    filename: `[name]-[hash].${lang}.min.js`,
  },
  plugins: [
    ...commonConfig[lang].plugins,
    new ExtractTextPlugin({ filename: `[name]-[hash].${lang}.min.css`, allChunks: true }),
    new webpack.optimize.UglifyJsPlugin({
      compressor: {
        warnings: false,
        screw_ie8: true
      }
    }),
    new BundleAnalyzerPlugin({
      generateStatsFile: true,
      analyzerMode: 'disabled' // Set to 'server' to analyze
    }),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV),
      'process.env.SEARCH_RESULT_LIMIT': 50
    })
  ],
  module: {
    ...commonConfig[lang].module,
    rules: [
      ...commonConfig[lang].module.rules,
      {
        test: /\.sass$/,
        loader: ExtractTextPlugin.extract({
          fallback: 'style-loader',
          use: [
            'css-loader',
            { loader: 'postcss-loader', options: { sourceMap: true } },
            'resolve-url-loader',
            {
              loader: 'sass-loader',
              options: {
                indentedSyntax: true,
                sourceMap: true, // Necessary for resolve-url
              }
            }
          ]
        })
      }
    ]
  }
}));
