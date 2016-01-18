
import React           from 'react'
import ReactDOM        from 'react-dom'
import PureRenderMixin from 'react/lib/ReactComponentWithPureRenderMixin'
import update          from 'react/lib/update'

export default function VDom(parentElement, transforms, getTempDiff){
    const Traverse = React.createClass({
        mixins: [PureRenderMixin],
        render(){
            const props = this.props
            const content = 
                props.chl ? props.chl.map(key => React.createElement(Traverse, props[key])) :
                props.at.content || null
            return React.createElement(props.at.tp, props.at, content)
        }
    })
    
    var incoming = null
    const diffOk = (data,diff) => diff && Object.keys(diff).every(
        key => key === "$set" || (key in data) && diffOk(data[key], diff[key])
    )
    function getMergedState(){
        const tempDiff = getTempDiff()
        const ok = diffOk(incoming,tempDiff)
        console.log(incoming,tempDiff,ok)
        const merged = ok ? update(incoming,tempDiff) : incoming
        return ({merged})
    }
    function updateMergedState(){
        const state = getMergedState()
        rootComponent.setState(state) 
    }
    const RootComponent = React.createClass({
        mixins: [PureRenderMixin],
        getInitialState(){ return getMergedState() },
        render(){ 
            return this.state.merged ? 
                React.createElement(Traverse,this.state.merged) : 
                React.createElement("div",null)
        }
    })
    function setupIncomingDiff(ctx) {
        Object.keys(ctx.value).forEach(key => {
            const value = ctx.value[key]
            if(transforms[key]) ctx.value[key] = transforms[key]({ value, parent: ctx })
            else if(key.substring(0,1)===":" || key === "at") setupIncomingDiff({ key, value, parent: ctx })
            else if(key === "$set") setupIncomingDiff({ value, parent: ctx })
        })
    }
    function showDiff(data){
        const diff = JSON.parse(data)
        setupIncomingDiff({ value: diff, updateMergedState })
        incoming = update(incoming || {}, diff)
        updateMergedState()
    }
    
    const rootNativeElement = document.createElement("div")
    parentElement.appendChild(rootNativeElement)
    const rootVirtualElement = React.createElement(RootComponent,null)
    const rootComponent = ReactDOM.render(rootVirtualElement, rootNativeElement)
    const receivers = {showDiff}
    return ({receivers})
}
