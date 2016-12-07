import React from 'react'
import IconButton        from 'material-ui/IconButton'
import SnackBar          from 'material-ui/Snackbar'


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
            marginTop:"38px",
            textAlign:this.props.alignRight?"right":"left"
        }
        Object.assign(pStyle,this.props.style)
        return(
            React.createElement("div",{key:"1",style:pStyle},[
                React.createElement("label",{key:"label",style:lStyle},this.props.label),
                React.createElement("div",{key:"text",style:tStyle},this.props.children)
            ]
            ))
    }
}

class IconButtonEx extends React.Component{
    constructor(props){
        super(props)
        this.state={zIndex:"auto"}
        this.handleMouseEnter=this.handleMouseEnter.bind(this)
        this.handleMouseLeave=this.handleMouseLeave.bind(this)
    }
    handleMouseEnter(){this.setState({zIndex:"4000"})}
    handleMouseLeave(){this.setState({zIndex:"auto"})}
    render(){
        const props={
            key:"iconButtonEx",
            onClick:this.props.onClick,
            tooltip:this.props.tooltip,
            style:Object.assign({zIndex:this.state.zIndex},this.props.style),
            iconStyle:this.props.iconStyle,
            onMouseEnter:this.handleMouseEnter,
            onMouseLeave:this.handleMouseLeave,
        }
        return React.createElement(IconButton,props,this.props.children)
    }
}

class SnackBarEx extends React.Component{
    constructor(props){
        super(props)
        this.handleRequestClose=this.handleRequestClose.bind(this)
    }
    handleRequestClose(){
        if(this.props.onRequestClose) this.props.onRequestClose()
    }
    render(){
        const props={
            key:"snackbar",
            onKeyDown:this.handleKeyDown,
            open:this.props.open,
            message:this.props.message,
            onRequestClose:this.handleRequestClose,
            action:this.props.action
        }
        return React.createElement(SnackBar,props)
    }
}

module.exports = {SnackBarEx,LabeledText,IconButtonEx}