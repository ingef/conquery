var path = require('path');
var webpack = require('webpack');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var StatsPlugin = require('stats-webpack-plugin');

module.exports = {
  entry: {
    main: path.join(__dirname, 'lib/js/main.js')
  },
  output: {
    path: path.join(__dirname, '/dist/'),
    filename: '[name]-[hash].min.js'
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: 'lib/index.tpl.html',
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
    new webpack.NoEmitOnErrorsPlugin(),
    new StatsPlugin('webpack.stats.json', {
      source: false,
      modules: false
    }),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV)
    })
  ],
  module: {
    rules: [{
      test: /\.js$/,
      exclude: /node_modules/,
      loader: 'babel-loader'
    }, {
      test: /\.json$/,
      loader: 'json-loader'
    }, {
      test: /\.yml$/,
      loader: 'json-loader!yaml-loader'
    }, {
      test: /\.sass$/,
      loader: ExtractTextPlugin.extract({
        fallback: "style-loader",
        use: "css-loader!postcss-loader!sass-loader?indentedSyntax"
      })
    }, {
      test: /\.woff(2)?(\?.*$|$)/,
      loader: "url-loader?name=[name].[ext]&limit=10000&minetype=application/font-woff"
    }, {
      test: /\.(ttf|eot|svg|png|jpg)(\?.*$|$)/,
      loader: "file-loader?name=[name].[ext]"
    }]
  }
};
