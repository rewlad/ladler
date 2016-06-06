
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
import IconContentSave  from 'material-ui/lib/svg-icons/content/save'
import IconActionDelete  from 'material-ui/lib/svg-icons/action/delete'
import IconActionRestore from 'material-ui/lib/svg-icons/action/restore'
import IconActionLock from 'material-ui/lib/svg-icons/action/lock'
import IconActionDateRange from 'material-ui/lib/svg-icons/action/date-range'
import IconContentFilterList from 'material-ui/lib/svg-icons/content/filter-list'
import IconContentRemove from 'material-ui/lib/svg-icons/content/remove'
import IconSocialPerson  from 'material-ui/lib/svg-icons/social/person'
import IconMenu          from 'material-ui/lib/menus/icon-menu'
import MenuItem          from 'material-ui/lib/menus/menu-item'
import IconNavigationMenu from 'material-ui/lib/svg-icons/navigation/menu'
import IconNavigationDropDown from 'material-ui/lib/svg-icons/navigation/arrow-drop-down'
import IconNavigationDropUp from 'material-ui/lib/svg-icons/navigation/arrow-drop-up'
import IconNavigationExpandMore from 'material-ui/lib/svg-icons/navigation/expand-more'
import IconNavigationExpandLess from 'material-ui/lib/svg-icons/navigation/expand-less'
import TextField         from 'material-ui/lib/TextField/TextField'
import DatePicker        from 'material-ui/lib/date-picker/date-picker'
import Checkbox          from 'material-ui/lib/checkbox'
import TimePicker        from 'material-ui/lib/time-picker/time-picker'
import MaterialChip      from '../main/material-chip'
import Calendar          from 'material-ui/lib/date-picker/calendar'
import injectTapEventPlugin from "react-tap-event-plugin"
injectTapEventPlugin()
function fixOnScrollBug(){
    document.body.style.overflowY="scroll"
}
fixOnScrollBug()
function fail(data){ alert(data) }

const feedback = Feedback()
const vdom = VDomMix(feedback)
const receivers = [feedback.receivers, vdom.receivers, {fail}]
SSEConnection("http://localhost:5556/sse", receivers, 5)

const DateInput = React.createClass({
    render(){
        const at = {
            floatingLabelText: this.props.floatingLabelText,
            errorText: this.props.errorText,
            //container: 'inline',
            locale: "et-EE",
            //mode:"landscape",
            disabled:this.props.disabled,
            DateTimeFormat: global.Intl.DateTimeFormat,
            textFieldStyle: this.props.style,
            underlineStyle: this.props.underlineStyle,
            onChange: (dummy,value) =>{ this.props.onChange({ target: { value: value.getTime().toString() }})}
        }
        const a=new Date()
        a.setTime(parseInt(this.props.value,10))
        console.log(a.toString())
        at.value = this.props.value ? new Date(parseInt(this.props.value,10)) : this.props.value
        return React.createElement(DatePicker, at, null)
    }
})
const TimeInput = React.createClass({
    render(){
        //const value=this.props.value? new Date(parseInt(this.props.value,10)):null
        var value=new Date()
        if(this.props.value){

            const hm=this.props.value.split(":")
            const h=parseInt(hm[0])
            const m=parseInt(hm[1])
            value.setHours(h)
            value.setMinutes(m)
        }
        else value=null
        const onChange=(dummy,value)=>{
            const h=value.getHours()<10?"0"+value.getHours().toString():value.getHours().toString()
            const m=value.getMinutes()<10?"0"+value.getMinutes().toString():value.getMinutes().toString()
            this.props.onChange({target:{value: h+":"+m}})
        }
        return React.createElement(TimePicker,{key:"1",floatingLabelText:this.props.floatingLabelText,
        underlineStyle: this.props.underlineStyle,
            format:"24hr",defaultTime:value,onChange:onChange,textFieldStyle:{width:"100%"}},null)
    }
})
/*
class DurationInput extends React.Component{
    constructor(props){
        super(props)
    }
    render(){
        return React.createElement(TimePicker,{key:"1",floatingLabelText:this.props.floatingLabelText, errorText: this.props.errorText,
                    format:"24hr",defaultTime:value,onChange:onChange,textFieldStyle:{width:"100%"}},null)
    }
}
*/
class FlexGridItemWidthSync extends React.Component{
    constructor(props){
        super(props)

    }
    componentDidMount(){
        const drect=ReactDOM.findDOMNode(this).getBoundingClientRect()
        this.props.onResize({width:drect.width,height:drect.height})
        console.log("dMounted");
    }
    componentWillReceiveProps(nextProps){

      if(nextProps.height!==this.props.height||nextProps.width!==this.props.width)
      if(typeof this.props.onResize ==="function"){

        this.props.onResize({width:nextProps.width,height:nextProps.height})
      }

    }
    render(){
        const tStyle={
            width:this.props.width||"100%",
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
    }

    render(){
        const pStyle={
            border:"0px solid black",
            fontSize:13,
            fontWeight:400,
            color:"rgba(0,0,0,0.87)",
            backgroundColor:this.props.selected?"#F5F5F5":this.state.mouseOver?"#eeeeee":"transparent"
        }

        return React.createElement("div",{key:this.props.key,style:pStyle,onMouseEnter:this.handleMouseEnter,
                        onMouseLeave:this.handleMouseLeave,onClick:this.props.onClick},this.props.children)
    }
}
class DataTableBody extends React.Component{
    constructor(props){
        super(props)

        this.dims=null
    }
    calcPosition(){
        this.dims=ReactDOM.findDOMNode(this).getBoundingClientRect()
    }
    componentDidMount(){
        this.calcPosition()
        console.log(this.dims)
    }
    componentWillUnmount(){}
    render(){
        const pStyle={
          //  backgroundColor:"dodgerBlue",
           // overflowY:"auto",
          //  height:"100px"
        }
        return React.createElement("div",{key:"1",style:pStyle},this.props.children)
    }
}
class LabeledText extends React.Component{
    constructor(props){
        super(props)
    }
    render(){
        const pStyle={
            position:"relative",
            display:"inline-block",
            fontSize:"16px",
            lineHeight:"24px",
            width:"100%",
            height:"72px"
        }
        const lStyle={
            position:"absolute",
            cursor:"text",
            pointerEvents:"none",
            lineHeight:"22px",
            transform: "perspective(1px) scale(0.75) translate3d(2px,-28px,0px)",
            transformOrigin:"left top 0px",
            top:"38px",
            userSelect:"none",
            color:"rgba(0,0,0,0.5)",
        }
        const tStyle={
            font:"inherit",
            height:"100%",
            color:"rgba(0,0,0,1)",
            position:"relative",
            marginTop:"38px"
        }

        return(
        React.createElement("div",{key:"1",style:pStyle},[
            React.createElement("label",{key:"label",style:lStyle},this.props.label),
            React.createElement("div",{key:"text",style:tStyle},this.props.children)

        ]))
    }
}

class IconMenuButton extends React.Component{
    constructor(props){
        super(props)
        this.handleRequestChange=this.handleRequestChange.bind(this)
    }
    handleRequestChange(open,reason){
        if(open===false)
            this.props.onClick()
        //console.log("aaa",open)

    }

    render(){
        return React.createElement(IconMenu,{key:this.props.key,onRequestChange:this.handleRequestChange,
            open:this.props.open,onTouchTap:this.props.onClick,
            iconButtonElement: React.createElement(IconButton,{key:"1",tooltip:"menu"}, React.createElement(IconNavigationMenu,{key:"1"}))},
            this.props.children
            )
    }
}

class CursorOver extends React.Component{
    constructor(props){
        super(props)
        this.state={mouseOver:false}
        this.handleMouseEnter=this.handleMouseEnter.bind(this)
        this.handleMouseLeave=this.handleMouseLeave.bind(this)
    }
    handleMouseEnter(){
        this.setState({mouseOver:true})
    }
    handleMouseLeave(){
        this.setState({mouseOver:false})
    }

    render(){
        const pStyle={
            backgroundColor:this.state.mouseOver?this.props.hoverColor:"transparent"
        }

        return React.createElement("div",{key:this.props.key,style:pStyle,onMouseEnter:this.handleMouseEnter,
                        onMouseLeave:this.handleMouseLeave},this.props.children)
    }
}

class CrazyCalendar extends React.Component{
    constructor(props){
        super(props)
        this.handleOnDayTouchTap=this.handleOnDayTouchTap.bind(this)
    }

    handleOnDayTouchTap(e,day){
        const utcDate=day.getTime()+day.getTimezoneOffset()*60*1000
        this.props.onChange({ target: { value: utcDate.toString() }})
    }

    render(){
    //console.log(global.Intl.DateTimeFormat)
        const propsCalender={
            key:"dialog",
            container:"dialog",
            ref:"calender",
            DateTimeFormat: global.Intl.DateTimeFormat,
            locale:"et-EE",
            mode:"portrait",
            onDayTouchTap:this.handleOnDayTouchTap,
            //onAccept:this.handleDialogAccept,
            initialDate:(new Date()),
            //open:false,
            //onShow={onShow}
            //onDismiss={onDismiss}
            //minDate={minDate}
            //maxDate={maxDate}
            //autoOk={autoOk}
            //disableYearSelection={disableYearSelection}
            //shouldDisableDate={this.props.shouldDisableDate}
            firstDayOfWeek:0
        }

        return React.createElement(Calendar,propsCalender)
    }
}


const tp = ({
    Paper,
    Table,TableHeader,TableBody,TableHeaderColumn,TableRow,TableRowColumn,
    RaisedButton,
    IconButton, IconContentCreate,MaterialChip,
    IconContentAdd,IconContentClear,IconContentFilterList,IconContentRemove,IconActionDelete,
    TextField, DateInput,TimeInput,Checkbox,DataTableRow,//DataTableBody,
    LabeledText,FlexGridItemWidthSync,IconActionLock,IconSocialPerson,
    IconActionRestore,IconContentSave,CrazyCalendar,//Calendar,
    IconMenuButton,MenuItem,IconNavigationMenu,CursorOver,
    IconNavigationDropDown,IconNavigationDropUp,IconActionDateRange,IconNavigationExpandMore,IconNavigationExpandLess
})

const transforms = ({tp})

vdom.transformBy({transforms})