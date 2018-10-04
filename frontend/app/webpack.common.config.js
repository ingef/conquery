const path = require('path');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = Object.assign({}, ...['en', 'de'].map(lang => ({
  [lang]: {
    plugins: [
      new HtmlWebpackPlugin({
        template: path.join(__dirname, 'src/index.tpl.html'),
        inject: 'body',
        filename: `index.${lang}.html`
      }),
      new webpack.NoEmitOnErrorsPlugin(),
      new webpack.ContextReplacementPlugin(/moment[\/\\]locale$/, /de/)
    ],
    module: {
      rules: [{
        test: /\.js$/,
        exclude: path.join(__dirname, '../node_modules/'),
        loader: 'babel-loader'
      }, {
        test: /\.json$/,
        loader: 'json-loader'
      }, {
        test: /\.yml$/,
        loader: 'json-loader!yaml-loader'
      }, {
        test: /\.(ttf|eot|svg|png|jpg|woff(2)?)(\?.*$|$)/,
        loader: "file-loader?name=[name].[ext]"
      }, {
        test: /\.css$/,
        loaders: ['style-loader', 'css-loader']
      }]
    }
  }
})));
