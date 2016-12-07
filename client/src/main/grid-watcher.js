import React from 'react'
import ReactDOM from 'react-dom'

const FlexGrid = React.createClass({
    render(){
        const style={
            display:"flex",
            flexWrap:"wrap",
            maxWidth:this.props.maxWidth||"100%",
            border:"0px solid black",
            justifyContent:"center",
            position:"relative",
            margin:"0px auto"
        }
        const ref = el => this.props.flexReg(false,el)
        return React.createElement("div",{ key:"flexGrid", style, ref }, this.props.children)
    }
})

const FlexGridShItem = React.createClass({
    render(){
        const style={
            flex:"1 1 "+this.props.flexBasis,
            border:"0px solid blue",
            maxWidth:(this.props.maxWidth),
            boxSizing:"border-box",
            margin:"0px 5px",
            height:(this.props.height||0)+"px"
        }
        const ref = el => this.props.flexReg(false,el)
        return React.createElement("div",{ key:"flexGridShItem", style, ref }, this.props.children)
    }
})

const FlexGridItem = React.createClass({
    render(){
        const style={
            position:"absolute",
            top:(this.props.y||"0")+"px",
            left:(this.props.x||"0")+"px",
            transition:this.props.noanim?"all 50ms ease-out":"all 300ms ease-out",
            boxSizing:"border-box",
            border:"0px solid black",
            width:(this.props.width||"0")+"px",
            textAlign:this.props.align||"left",
        }
        const ref = el => this.props.flexReg(true,el)
        return React.createElement("div",{ key: "flexGridItem", style, ref }, this.props.children)
    }
})
class FlexGridItemWidthSync extends React.Component{
    constructor(props){
        super(props)
    }
    componentDidMount(){
        const drect=ReactDOM.findDOMNode(this).getBoundingClientRect()
        this.props.onResize({width:drect.width,height:drect.height})
    }
    componentWillReceiveProps(nextProps){
      if(nextProps.height!==this.props.height||nextProps.width!==this.props.width)
      if(typeof this.props.onResize ==="function"){
        this.props.onResize({width:nextProps.width,height:nextProps.height})
      }
    }
    render(){
        const tStyle={
            width:this.props.width+"px"||"100%",
            top:this.props.y+"px"||"0px",
            left:this.props.x+"px"||"0px",
            transition:"all 300ms ease-out",
             position:"absolute"
        }
        if(this.props.minWidth) Object.assign(tStyle,{minWidth:this.props.minWidth})
        if(this.props.maxWidth) Object.assign(tStyle,{maxWidth:this.props.maxWidth})
        Object.assign(tStyle,this.props.style||{})
        const ref = el => this.props.flexReg(true,el)

        return React.createElement("div",{key:"flexGridItemWithSync",style:tStyle,ref},this.props.children)
    }
}

export default function GridWatcher(vDom, DiffPrepare){
    const ref_collection = {}
    function layoutIteration(){
        const refs = Object.keys(ref_collection).map(key=>ref_collection[key])
        refs.forEach(refc=>{
	    
            const drect = refc.element.getBoundingClientRect()
            refc.width = drect.width
            refc.height = drect.height
            refc.x = drect.left
            refc.y = drect.top	    
        })
        var diff = DiffPrepare(vDom.localState)
        refs.forEach(refc=>{
            if(!refc.isItem) return;
            const shadow = ref_collection[refc.parent_path_str]
            if(!shadow) return;
            const grid = ref_collection[shadow.parent_path_str]
            if(!grid) return;
            diff.jump(shadow.path)
            diff.addIfChanged("height", refc.height.toFixed(2))
            diff.jump(refc.path)
            diff.addIfChanged("width", shadow.width.toFixed(2))
            diff.addIfChanged("x", (shadow.x-grid.x).toFixed(2))
            diff.addIfChanged("y", (shadow.y-grid.y).toFixed(2))
        })
        diff.apply()
    }
    function transformReg(ctx){
        const path = vDom.ctxToArray(ctx,[])
        const path_str = path.join("/")
        const parent_path = path.slice(0)
        parent_path.splice(-2,1)
        const parent_path_str = parent_path.join("/")
        return function register(isItem,element) {
		
            if(element) ref_collection[path_str] = {path,parent_path_str,element,isItem} 
            else delete ref_collection[path_str]
        }
    }
    setInterval(layoutIteration, 50)
    const flexReg = { "def": transformReg }
    const tp = {FlexGrid,FlexGridItem,FlexGridShItem,FlexGridItemWidthSync}
    const transforms = {flexReg,tp}
    return ({transforms})
}
