
"use strict";

import SSEConnection from "../main/sse-connection"
import Feedback      from "../main/feedback"
import VDomMix       from "../main/vdom-mix"

import React             from 'react'
import ReactDOM          from 'react-dom'

import Paper             from 'material-ui/lib/paper'
import Table             from 'material-ui/lib/table/table'
import TableHeader       from 'material-ui/lib/table/table-header'
import TableBody         from 'material-ui/lib/table/table-body'
import TableHeaderColumn from 'material-ui/lib/table/table-header-column'
import TableRow          from 'material-ui/lib/table/table-row'
import TableRowColumn    from 'material-ui/lib/table/table-row-column'
import RaisedButton      from 'material-ui/lib/raised-button'
import IconButton        from 'material-ui/lib/icon-button'
import IconContentCreate from 'material-ui/lib/svg-icons/content/create'
import IconContentAdd    from 'material-ui/lib/svg-icons/content/add'
import IconContentClear  from 'material-ui/lib/svg-icons/content/clear'
import IconContentFilterList from 'material-ui/lib/svg-icons/content/filter-list'
import IconContentRemove from 'material-ui/lib/svg-icons/content/remove'
import TextField         from 'material-ui/lib/TextField/TextField'
import DatePicker        from 'material-ui/lib/date-picker/date-picker'
import Checkbox          from 'material-ui/lib/checkbox'
import TimePicker        from 'material-ui/lib/time-picker/time-picker'
import MaterialChip      from '../main/material-chip'
import injectTapEventPlugin from "react-tap-event-plugin"
injectTapEventPlugin()

function fail(data){ alert(data) }

const feedback = Feedback()
const vdom = VDomMix(feedback)
const receivers = [feedback.receivers, vdom.receivers, {fail}]
SSEConnection("http://localhost:5556/sse", receivers, 5)

const DateInput = React.createClass({
    render(){
        const at = {
            floatingLabelText: this.props.floatingLabelText,
            //container: 'inline',
            //locale: "ee",
            disabled:this.props.disabled,
            DateTimeFormat: global.Intl.DateTimeFormat,
            textFieldStyle: this.props.style,
            onChange: (dummy,value) => this.props.onChange({ target: { value: value.getTime().toString() }})
        }
        at.value = this.props.value ? new Date(parseInt(this.props.value,10)) : this.props.value
        return React.createElement(DatePicker, at, null)
    }
})
const TimeInput = React.createClass({
    render(){
        const value=this.props.value? new Date(parseInt(this.props.value,10)):this.props.value

        const onChange=(dummy,value)=>{

            this.props.onChange({target:{value: value.getTime().toString()}})
        }
        return React.createElement(TimePicker,{key:"1",floatingLabelText:this.props.floatingLabelText,
            format:"24hr",defaultTime:value,onChange:onChange,textFieldStyle:{width:"100%"}},null)
    }
})
class DataTable extends React.Component{
    constructor(props){
        super(props)

    }
    componentDidMount(){
        const drect=ReactDOM.findDOMNode(this).getBoundingClientRect()
        //console.log(drect)
        this.props.onResize({width:drect.width,height:drect.height})
        console.log("dMounted");
    }
    componentWillUnmount(){}
    componentWillReceiveProps(nextProps){

      if(nextProps.height!==this.props.height||nextProps.width!==this.props.width)
      if(typeof this.props.onResize ==="function"){

        this.props.onResize({width:nextProps.width,height:nextProps.height})
      }

    }
    handleResize(){
      //  console.log("reize")
    }


    render(){
        const tStyle={
            width:this.props.width||"100%",
           // height:this.props.height||"",
            top:this.props.y||"0px",
            left:this.props.x||"0px",
            transition:"all 300ms ease-out",
             position:"absolute"
        }
        if(this.props.minWidth) Object.assign(tStyle,{minWidth:this.props.minWidth})
        if(this.props.maxWidth) Object.assign(tStyle,{maxWidth:this.props.maxWidth})
        Object.assign(tStyle,this.props.style||{})
        const ref = el => this.props.flexReg(true,el)

        return React.createElement("div",{key:this.props.key,style:tStyle,ref},this.props.children)
    }
}

var leave
class DataTableRow extends React.Component{
    constructor(props){
        super(props)
        this.state={mouseOver:false}
        this.handleMouseEnter=this.handleMouseEnter.bind(this)
        this.handleMouseLeave=this.handleMouseLeave.bind(this)
    }
    handleMouseEnter(){

        if(leave) leave()
        leave = ()=>this.setState({mouseOver:false})
        this.setState({mouseOver:true})
    }
    handleMouseLeave(){
        if(leave) leave()
        leave = null

        //this.setState({mouseOver:false})
    }

    render(){
        const pStyle={
            display:"table",
            width:"100%",
            height:"100%",
            border:"0px solid black",
            //fontFamily: 'Roboto, sans-serif',
            fontSize:13,
            fontWeight:400,
            color:"rgba(0,0,0,0.87)",
            backgroundColor:this.props.selected?"#F5F5F5":this.state.mouseOver?"#eeeeee":"transparent"
        }

        return React.createElement("div",{key:this.props.key,style:pStyle,onMouseEnter:this.handleMouseEnter,
                        onMouseLeave:this.handleMouseLeave},this.props.children)
    }
}

class DataTableCells extends React.Component{
    constructor(props){
        super(props)
    }
    render(){
        const pStyle={
            display:"table-cell"
        }

        return React.createElement("div",{key:this.props.key,style:pStyle,onClick:this.props.onClick},this.props.children)
    }
}
class LabeledText extends React.Component{
    constructor(props){
        super(props)
    }
    render(){
        const pStyle={
            position:"relative"

        }
        const lStyle={
            fontSize:"12px",
            position:"absolute",
            top:"0px",
            userSelect:"none",
            paddingTop:"8px",
            color:"rgba(128,128,128,1)",
            display:"block",
        }
        const tStyle={
           // fontSize:"16px",
            paddingTop:"30px",
            color:"rgba(0,0,0,1)"
        }

        return(
        React.createElement("div",{key:"1",style:pStyle},[
            React.createElement("div",{key:"label",style:lStyle},this.props.label),
            React.createElement("div",{key:"text",style:tStyle},this.props.children)

        ]))
    }
}
const tp = ({
    Paper,
    Table,TableHeader,TableBody,TableHeaderColumn,TableRow,TableRowColumn,
    RaisedButton,
    IconButton, IconContentCreate,MaterialChip,
    IconContentAdd,IconContentClear,IconContentFilterList,IconContentRemove,
    TextField, DateInput,TimeInput,Checkbox,DataTable,DataTableRow,DataTableCells,
    LabeledText
})

const transforms = ({tp})

vdom.transformBy({transforms})