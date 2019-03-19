const path = require("path");
const webpack = require("webpack");
const ProgressBarPlugin = require("progress-bar-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const OptimizeCSSAssetsPlugin = require("optimize-css-assets-webpack-plugin");
const TerserPlugin = require("terser-webpack-plugin");
const { getIfUtils, removeEmpty } = require("webpack-config-utils");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

const env = process.env.NODE_ENV || "development";
const { ifProduction, ifDevelopment } = getIfUtils(env);

module.exports = ["en", "de"].map(lang => ({
  mode: env,
  name: lang,
  devtool: ifDevelopment("eval-source-map", "source-map"),
  entry: {
    main: removeEmpty([
      "@babel/polyfill",
      ifDevelopment("webpack-hot-middleware/client?reload=true"),
      path.join(__dirname, `src/js/main.${lang}.js`)
    ])
  },
  output: {
    path: path.join(__dirname, "dist"),
    filename: `[name].${lang}.js`,
    publicPath: ifProduction("/app/static/", "/")
  },
  optimization: {
    minimizer: removeEmpty([
      ifProduction(
        new TerserPlugin({
          parallel: true,
          sourceMap: true
        })
      ),
      ifProduction(new OptimizeCSSAssetsPlugin())
    ])
  },
  plugins: removeEmpty([
    ifProduction(
      new MiniCssExtractPlugin({
        filename: `[name]-[hash].${lang}.min.css`
      })
    ),
    ifDevelopment(new webpack.HotModuleReplacementPlugin()),
    ifDevelopment(new ProgressBarPlugin()),
    new webpack.DefinePlugin({
      "process.env.SEARCH_RESULT_LIMIT": 50
    }),
    new HtmlWebpackPlugin({
      template: path.join(__dirname, "src/index.tpl.html"),
      inject: "body",
      filename: `index.${lang}.html`,
      publicPath: ifProduction("/app/static/", "/")
    })
  ]),
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: path.join(__dirname, "../node_modules/"),
        use: "babel-loader"
      },
      {
        test: /\.yml$/,
        use: "js-yaml-loader"
      },
      {
        test: /\.(ttf|eot|svg|png|jpg|woff(2)?)(\?.*$|$)/,
        use: "file-loader?name=[name].[ext]"
      },
      {
        test: /\.(sa|sc|c)ss$/,
        use: [
          ifProduction(MiniCssExtractPlugin.loader, "style-loader"),
          "css-loader",
          { loader: "postcss-loader", options: { sourceMap: true } },
          "resolve-url-loader",
          {
            loader: "sass-loader",
            options: {
              sourceMap: true // Necessary for resolve-url
            }
          }
        ]
      }
    ]
  },
  node: {
    fs: "empty"
  }
}));
