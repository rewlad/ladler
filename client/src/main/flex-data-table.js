import React from 'react'

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
            fontSize:"13px",
            fontWeight:400,
            color:"rgba(0,0,0,0.87)",
            backgroundColor:this.props.selected?"#F5F5F5":this.state.mouseOver?"#eeeeee":"transparent"
        }
        return React.createElement("div",{key:"dataTableRow",style:pStyle,onMouseEnter:this.handleMouseEnter,
                        onMouseLeave:this.handleMouseLeave,onClick:this.props.onClick},this.props.children)
    }
}

module.exports = {DataTableRow}