
import React           from 'react'
import ReactDOM        from 'react-dom'
import PureRenderMixin from 'react/lib/ReactComponentWithPureRenderMixin'
import update          from 'react/lib/update'

export default function VDom(parentElement, transforms){
    const Traverse = React.createClass({
        mixins: [PureRenderMixin],
        render(){
            const props = this.props
            const childElements = !props.chl ? null : 
                props.chl.map(key => React.createElement(Traverse, props[key]))
            console.log(props)
            return React.createElement(props.tp, props.at || props, childElements)
        }
    })
    
    var incoming = { tp: "div" }, tempDiff
    function getMergedState(){ 
        return { merged: tempDiff ? update(incoming,tempDiff) : incoming } 
    }
    const RootComponent = React.createClass({
        mixins: [PureRenderMixin],
        getInitialState(){ return getMergedState() },
        render(){ return React.createElement(Traverse,this.state.merged) }
    })
    function setTempDiff(diff){
        tempDiff = diff
        rootComponent.setState(getMergedState())
    }
    function typePos(key){ return key.lastIndexOf(":") + 1 }
    function setupIncomingDiff(ctx) {
        Object.keys(ctx.value).forEach(key => {
            const value = ctx.value[key]
            if(transforms[key]) ctx.value[key] = transforms[key]({ value, parent: ctx })
            else if(typePos(key) > 0) setupIncomingDiff({ key, value, parent: ctx })
            else if(key === "$set") setupIncomingDiff({ value, parent: ctx })
        })
    }
    function showDiff(data){
        const diff = JSON.parse(data)
        setupIncomingDiff({ value: diff, setTempDiff })
        incoming = update(incoming, diff)
        rootComponent.setState(getMergedState())
    }
    
    const rootNativeElement = document.createElement("div")
    parentElement.appendChild(rootNativeElement)
    const rootVirtualElement = React.createElement(RootComponent,null)
    const rootComponent = ReactDOM.render(rootVirtualElement, rootNativeElement)
    const receivers = {showDiff}
    return ({receivers})
}
