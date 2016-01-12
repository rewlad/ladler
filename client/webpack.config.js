
var HtmlWebpackPlugin = require('html-webpack-plugin')

function config(name) {
    return {
        entry: "./src/"+name+".js",
        output: {
            path: "build",
            filename: name + ".js"
        },
        plugins: [new HtmlWebpackPlugin({
            filename: name + ".html",
            title: name,
            hash: true,
            favicon: "./src/favicon.png"
        })]
    }
}

module.exports = [
    config("hello"),
    config("app")
]
