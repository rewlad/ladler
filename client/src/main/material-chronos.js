import React             from 'react'
import Calendar          from 'material-ui/DatePicker/Calendar'
import Clock             from 'material-ui/TimePicker/Clock'

class CrazyClock extends React.Component{
    constructor(props){
        super(props)
        this.handleClockChangeMinutes=this.handleClockChangeMinutes.bind(this)
    }
    handleClockChangeMinutes(){
        const time=this.refs.clock.getSelectedTime()
        const value = time.getHours()+":"+time.getMinutes()
        this.props.onChange({ target: ({value}) })
    }
    render(){
        const initialTime=new Date()
        if(this.props.initialTime){
            const hm=this.props.initialTime.split(":")
            initialTime.setHours(parseInt(hm[0]))
            initialTime.setMinutes(parseInt(hm[1]))
        }
        const format="24hr"
        const propsClock={
            key: "clock",
            ref: 'clock',
            format,
            initialTime,
            onChangeMinutes: this.handleClockChangeMinutes
        }
        return React.createElement(Clock,propsClock)
    }
}

class CrazyCalendar extends React.Component{
    constructor(props){
        super(props)
        this.handleOnDayTouchTap=this.handleOnDayTouchTap.bind(this)
    }
    handleOnDayTouchTap(e,day){
        const value = day.getDate()+"."+(day.getMonth()+1)+"."+day.getFullYear()
        this.props.onChange({ target: ({value}) })
    }
    render(){
        const initialDate=new Date()
        if(this.props.initialDate){
            const dmy=this.props.initialDate.split(".")
            initialDate.setDate(parseInt(dmy[0]))
            initialDate.setMonth(parseInt(dmy[1])-1)
            initialDate.setFullYear(parseInt(dmy[2]))
        }
        const propsCalender={
            key:"calendar",
            container:"dialog",
            ref:"calender",
            DateTimeFormat: global.Intl.DateTimeFormat,
            locale:"et-EE",
            mode:"portrait",
            onTouchTapDay:this.handleOnDayTouchTap,
            initialDate,
            firstDayOfWeek:0
        }
        return React.createElement(Calendar,propsCalender)
    }
}

module.exports = {CrazyCalendar,CrazyClock}