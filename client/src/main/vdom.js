
import React           from 'react'
import ReactDOM        from 'react-dom'
import PureRenderMixin from 'react/lib/ReactComponentWithPureRenderMixin'
import update          from 'react/lib/update'

export default function VDom(parentElement, componentClasses, transforms){
    function typePos(key){ return key.lastIndexOf(":") + 1 }
    const Traverse = React.createClass({
        mixins: [PureRenderMixin],
        render(){
            const props = this.props
            const type = componentClasses[props.typeStr] || props.typeStr
            const childKeys = !props.incoming ? null : props.incoming["chl"]
            const childElements = !childKeys ? null : childKeys.map(key => {
                const typeStr = key.substring(key.lastIndexOf(":")+1)
                const incoming = props.incoming[key]
                return React.createElement(Traverse, {key, typeStr, incoming})
            })
            const attributes = !props.incoming ? null : props.incoming["at"] ||
                (props.incoming["chl"] ? null : props.incoming)
            console.log(type, attributes)
            return React.createElement(type, attributes, childElements)
        }
    })
    const RootComponent = React.createClass({
        getInitialState(){ return { typeStr: "div" } },
        render(){ return React.createElement(Traverse,this.state) }
    })
    function setup(ctx) {
        Object.keys(ctx.value).forEach(key => {
            const value = ctx.value[key]
            if(transforms[key]) ctx.value[key] = transforms[key]({ value, parent: ctx })
            else if(typePos(key) > 0) setup({ key, value, parent: ctx })
            else if(key === "$set") setup({ value, parent: ctx })
        })
    }
    function showDiff(data){
        const diff = JSON.parse(data)
        setup({ value: diff })
        const incoming = update(rootComponent.state.incoming||{}, diff)
        rootComponent.setState({incoming})
    }
    const rootNativeElement = document.createElement("div")
    parentElement.appendChild(rootNativeElement)
    const rootVirtualElement = React.createElement(RootComponent,null)
    const rootComponent = ReactDOM.render(rootVirtualElement, rootNativeElement)
    const receivers = {showDiff}
    return ({receivers})
}
