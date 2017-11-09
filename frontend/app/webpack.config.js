var path = require('path');
var webpack = require('webpack');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');

module.exports = {
  devtool: 'source-map',
  entry: {
    main: [
      'webpack-hot-middleware/client?reload=true',
      path.join(__dirname, 'src/js/main.js')
    ]
  },
  output: {
    path: path.join(__dirname, '/dist/'),
    filename: '[name].js',
    publicPath: '/'
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: 'app/src/index.tpl.html',
      inject: 'body',
      filename: 'index.html'
    }),
    new webpack.HotModuleReplacementPlugin(),
    new webpack.NoEmitOnErrorsPlugin(),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('development')
    }),
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
      loader: "style-loader!css-loader!postcss-loader!sass-loader?indentedSyntax"
    }, {
      test: /\.woff(2)?(\?.*$|$)/,
      loader: "url-loader?name=[name].[ext]&limit=10000&minetype=application/font-woff"
    }, {
      test: /\.(ttf|eot|svg|png|jpg)(\?.*$|$)/,
      loader: "file-loader?name=[name].[ext]"
    }]
  },
  node: {
    fs: "empty"
  }
};
