import React from 'react'

class KeyboardReceiver extends React.Component{
    constructor(props){
        super(props)
        this.handleKeyDown=this.handleKeyDown.bind(this)
    }
    handleKeyDown(e){
        if(this.props.send){
            if(e.keyCode!=this.props.keyCode) return
            e.target.value=e.keyCode
            this.props.onChange(e)
        }
    }
    componentWillMount(){
        document.addEventListener("keydown",this.handleKeyDown)
    }
    componentWillUnmount(){
        document.removeEventListener("keydown",this.handleKeyDown)
    }
    render(){
        return React.createElement("div",{key:"keyboardListener"})
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
        return React.createElement("div",{key:"cursorOver",style:pStyle,onMouseEnter:this.handleMouseEnter,
                        onMouseLeave:this.handleMouseLeave},this.props.children)
    }
}

module.exports = {KeyboardReceiver,CursorOver}