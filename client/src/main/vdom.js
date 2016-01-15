
import React           from 'react'
import ReactDOM        from 'react-dom'
import PureRenderMixin from 'react/lib/ReactComponentWithPureRenderMixin'
import update          from 'react/lib/update'

export default function VDom(parentElement, transforms, getTempDiff){
    const Traverse = React.createClass({
        mixins: [PureRenderMixin],
        render(){
            const props = this.props
            const childElements = !props.chl ? null : 
                props.chl.map(key => React.createElement(Traverse, props[key]))
            const attributes = props.at || props
            console.log(props)
            return React.createElement(attributes.tp, attributes, childElements || attributes.content)
        }
    })
    
    var incoming = { tp: "div" }
    function getMergedState(){
        const tempDiff = getTempDiff()
        return { merged: tempDiff ? update(incoming,tempDiff) : incoming } 
    }
    function updateMergedState(){ rootComponent.setState(getMergedState()) }
    const RootComponent = React.createClass({
        mixins: [PureRenderMixin],
        getInitialState(){ return getMergedState() },
        render(){ return React.createElement(Traverse,this.state.merged) }
    })
    function setupIncomingDiff(ctx) {
        Object.keys(ctx.value).forEach(key => {
            const value = ctx.value[key]
            if(transforms[key]) ctx.value[key] = transforms[key]({ value, parent: ctx })
            else if(key.substring(0,1)===":") setupIncomingDiff({ key, value, parent: ctx })
            else if(key === "$set") setupIncomingDiff({ value, parent: ctx })
        })
    }
    function showDiff(data){
        const diff = JSON.parse(data)
        setupIncomingDiff({ value: diff, updateMergedState })
        incoming = update(incoming, diff)
        updateMergedState()
    }
    
    const rootNativeElement = document.createElement("div")
    parentElement.appendChild(rootNativeElement)
    const rootVirtualElement = React.createElement(RootComponent,null)
    const rootComponent = ReactDOM.render(rootVirtualElement, rootNativeElement)
    const receivers = {showDiff}
    return ({receivers})
}
