const path = require('path');
const webpack = require('webpack');
const { getIfUtils, removeEmpty } = require('webpack-config-utils');
const ProgressBarPlugin = require('progress-bar-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;

const { ifProduction, ifDevelopment } = getIfUtils(process.env.NODE_ENV || 'development');

module.exports = ['en', 'de'].map(lang => ({
  name: lang,
  devtool: ifDevelopment('eval-source-map'),
  entry: {
    main: removeEmpty([
      'babel-polyfill',
      ifDevelopment('webpack-hot-middleware/client?reload=true'),
      path.join(__dirname, `src/js/main.${lang}.js`)
    ])
  },
  output: {
    path: path.join(__dirname, 'dist/'),
    filename: ifProduction(`[name]-[hash].${lang}.min.js`, `[name].${lang}.js`),
    publicPath: '/',
    pathinfo: ifDevelopment(true, false)
  },
  plugins: removeEmpty([
    new HtmlWebpackPlugin({
      template: path.join(__dirname, 'src/index.tpl.html'),
      inject: 'body',
      filename: `index.${lang}.html`
    }),
    new webpack.ContextReplacementPlugin(/moment[\/\\]locale$/, /de/),
    ifProduction(new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV)
    })),
    ifProduction(new ExtractTextPlugin({
      filename: `[name]-[hash].${lang}.min.css`, allChunks: true
    })),
    ifProduction(new webpack.optimize.ModuleConcatenationPlugin()),
    ifProduction(new webpack.optimize.UglifyJsPlugin({
      compressor: {
        warnings: false,
        screw_ie8: true
      }
    })),
    ifProduction(new BundleAnalyzerPlugin({
      generateStatsFile: true,
      analyzerMode: 'static' // Set to 'server' to analyze
    })),
    ifProduction(new webpack.NoEmitOnErrorsPlugin()),
    ifDevelopment(new ProgressBarPlugin()),
    ifDevelopment(new webpack.HotModuleReplacementPlugin()),
  ]),
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: path.join(__dirname, '../node_modules/'),
        loader: 'babel-loader',
        options: { cacheDirectory: '../.babel_loader_cache' }
      }, {
        test: /\.json$/,
        loader: 'json-loader'
      }, {
        test: /\.yml$/,
        loader: 'json-loader!yaml-loader'
      }, {
        test: /\.(ttf|eot|svg|png|jpg|woff(2)?)(\?.*$|$)/,
        loader: "file-loader?name=[name].[ext]"
      },
      {
        test: /\.sass$/,
        use: ifProduction(
          ExtractTextPlugin.extract({
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
          }),
          [
          'style-loader',
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
        ])
      },
    ]
  },
  node: {
    fs: 'empty'
  }
}));
