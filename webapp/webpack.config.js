const path = require("path");
const HtmlWebpackPlugin = require("html-webpack-plugin");

module.exports = {
  entry: "./src/index.tsx",
  output: {
    path: path.resolve(__dirname, "dist"),
    filename: "bundle.js",
    publicPath: "/"
  },
  module: {
    rules: [
      {
        test: /\.(js|jsx|ts|tsx)$/,
        exclude: /node_modules/,
        use: {
          loader: "babel-loader"
        }
      },
      {
        // MUI 9 ships ESM (.mjs) with bare, extensionless imports (e.g.
        // 'react-transition-group/TransitionGroupContext'). Webpack 5 treats those
        // as "fully specified" and refuses to resolve them; disabling that lets the
        // extensionless imports resolve normally.
        test: /\.m?js$/,
        resolve: {
          fullySpecified: false
        }
      },
      {
        test: /\.css$/,
        use: ["style-loader", "css-loader"]
      }
    ]
  },
  resolve: {
    extensions: [".js", ".jsx", ".ts", ".tsx"]
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: "./public/index.html"
    })
  ],
  devServer: {
    historyApiFallback: true,
    port: 3000,
    proxy: [
      {
        context: ["/api"],
        target: "http://localhost:7070"
      }
    ]
  }
};
