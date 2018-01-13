const path = require('path');
const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const CompressionPlugin = require("compression-webpack-plugin")
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;

const commonConfig = require('./webpack.common.config.js');

module.exports = env => {
  const useBundleAnalyzer = env && env.stats;
  return {
    ...commonConfig,
    entry: {
      main: path.join(__dirname, 'src/js/main.js')
    },
    output: {
      path: path.join(__dirname, '/dist/'),
      filename: '[name]-[hash].min.js'
    },
    plugins: [
      ...commonConfig.plugins,
      new ExtractTextPlugin({filename: '[name]-[hash].min.css', allChunks: true}),
      new webpack.optimize.UglifyJsPlugin({
        compressor: {
          warnings: false,
          screw_ie8: true
        }
      }),
      new CompressionPlugin(),
      new BundleAnalyzerPlugin({
        generateStatsFile: true,
        analyzerMode: 'disabled' // Set to 'server' to analyze
      }),
      new webpack.DefinePlugin({
        'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV)
      })
    ],
    module: {
      ...commonConfig.module,
      rules: [
        ...commonConfig.module.rules,
        {
          test: /\.sass$/,
          loader: ExtractTextPlugin.extract({
            fallback: 'style-loader',
            use: [
              'css-loader',
              'postcss-loader',
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
  }
};
