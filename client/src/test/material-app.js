
"use strict";

import SSEConnection from "../main/sse-connection"
import Feedback      from "../main/feedback"
import VDomMix       from "../main/vdom-mix"

import React             from 'react'

import Paper             from 'material-ui/lib/paper'
import Table             from 'material-ui/lib/table/table'
import TableHeader       from 'material-ui/lib/table/table-header'
import TableBody         from 'material-ui/lib/table/table-body'
import TableHeaderColumn from 'material-ui/lib/table/table-header-column'
import TableRow          from 'material-ui/lib/table/table-row'
import TableRowColumn    from 'material-ui/lib/table/table-row-column'
import RaisedButton      from 'material-ui/lib/raised-button'
import IconButton        from 'material-ui/lib/icon-button'
import IconContentAdd    from 'material-ui/lib/svg-icons/content/add'
import IconContentClear  from 'material-ui/lib/svg-icons/content/clear'
import IconContentFilterList from 'material-ui/lib/svg-icons/content/filter-list'
import IconContentRemove from 'material-ui/lib/svg-icons/content/remove'
import TextField         from 'material-ui/lib/TextField/TextField'
import DatePicker        from 'material-ui/lib/date-picker/date-picker'

function fail(data){ alert(data) }

const feedback = Feedback()
const vdom = VDomMix(feedback)
const receivers = [feedback.receivers, vdom.receivers, {fail}]
SSEConnection("http://localhost:5556/sse", receivers, 5)

const DateInput = React.createClass({
    render(){
        const at = {
            floatingLabelText: this.props.floatingLabelText,
            container: 'dialog',
            textFieldStyle: this.props.style,
            onChange: (dummy,value) => this.props.onChange(value.getTime().toString())
        }
        if(this.props.value) at.date = new Date(parseInt(this.props.value,10))
        return React.createElement("DatePicker", at, content)
    }
})

const tp = ({
    Paper,
    Table,TableHeader,TableBody,TableHeaderColumn,TableRow,TableRowColumn,
    RaisedButton,
    IconButton, 
    IconContentAdd,IconContentClear,IconContentFilterList,IconContentRemove,
    TextField, DatePicker
})
const transforms = ({tp})
vdom.transformBy({transforms})