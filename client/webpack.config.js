
var HtmlWebpackPlugin = require('html-webpack-plugin')

function config(kind,name) {
    return {
        entry: "./src/"+kind+"/"+name+".js",
        output: {
            path: "build/"+kind,
            filename: name + ".js"
        },
        module: { loaders: [
            {
                test: /\.jsx?$/,
                exclude: /(node_modules|bower_components)/,
                loader: "babel",
                query: {
                    plugins: ["transform-es2015-modules-commonjs"]
                }
            }
        ]},
        plugins: [new HtmlWebpackPlugin({
            filename: name + ".html",
            title: name,
            hash: true,
            favicon: "./src/main/favicon.png"
        })]
    }
}

module.exports = [
    config("test","app"),
    config("test","btn"),
    config("test","sse"),
    config("test","hello")
]
