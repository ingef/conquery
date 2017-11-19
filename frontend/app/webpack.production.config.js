const path = require('path');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const StatsPlugin = require('stats-webpack-plugin');

const conqueryConfig = require('../webpack.config.js');

module.exports = {
  ...conqueryConfig,
  entry: {
    main: path.join(__dirname, 'src/js/main.js')
  },
  output: {
    path: path.join(__dirname, '/dist/'),
    filename: '[name]-[hash].min.js'
  },
  plugins: [
    ...conqueryConfig.plugins,
    new HtmlWebpackPlugin({
      template: 'src/index.tpl.html',
      inject: 'body',
      filename: 'index.html'
    }),
    new ExtractTextPlugin({ filename: "[name]-[hash].min.css", allChunks: true }),
    new webpack.optimize.UglifyJsPlugin({
      compressor: {
        warnings: false,
        screw_ie8: true
      }
    }),
    new StatsPlugin('webpack.stats.json', {
      source: false,
      modules: false
    }),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV)
    })
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
    }, {
      test: /\.sass$/,
      loader: ExtractTextPlugin.extract({
        fallback: "style-loader",
        use: "css-loader!postcss-loader!sass-loader?indentedSyntax"
      })
    }]
  }
};
