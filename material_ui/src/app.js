"use strict";

console.log("hello")

const React = require('react')
const ReactDOM = require('react-dom')
const RaisedButton = require('material-ui/lib/raised-button')

ReactDOM.render(React.createElement(RaisedButton,{label:"HELLO"}), document.getElementById("app"))

