"use strict";

import React from 'react'
import ReactDOM from 'react-dom'

/*
const React = require('react')
const ReactDOM = require('react-dom')
const RaisedButton = require('material-ui/lib/raised-button')
*/

/*
import RaisedButton from 'material-ui/lib/raised-button'
ReactDOM.render(React.createElement(RaisedButton,{label:"HELLO"}), document.getElementById("app"))
*/

import ui from "material-ui/lib"

const appElement = document.createElement("div")
document.body.appendChild(appElement)

ReactDOM.render(React.createElement(ui.RaisedButton,{label:"HELLO"}), appElement)


