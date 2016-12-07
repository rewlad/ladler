import React from 'react'
import baseTheme from 'material-ui/styles/baseThemes/lightBaseTheme'
import getMuiTheme from 'material-ui/styles/getMuiTheme'

const MuiThemeParent = React.createClass({
    getChildContext(){
        return { muiTheme: getMuiTheme(baseTheme)}
    },
    render(){
        return React.createElement("div",{key:"muiTheme"},this.props.children)
    }
})
MuiThemeParent.childContextTypes = {
    muiTheme: React.PropTypes.object.isRequired
}

module.exports = {MuiThemeParent}