"use strict";

import React from 'react'
import ReactDOM from 'react-dom'
import ui from "material-ui/lib"
import ReactGridLayout from "react-grid-layout"

const m = React.createElement


const Login = () => m(
    ReactGridLayout,
    {
        key:1,
        cols: 3,
        className: "layout",
        //rowHeight: 300,
        verticalCompact: false,
        layout: [{i:1,x:1,y:2,w:1,h:1}]
    },[
        m(ui.Paper,{key:1, margin: 20},[
            m(ui.RaisedButton,{label:"LOGIN", key:1 })
        ])
    ]
)

const viewByHash = ({Login})


const appElement = document.createElement("div")
document.body.appendChild(appElement)
ReactDOM.render(viewByHash[location.hash.substring(1)](), appElement)



