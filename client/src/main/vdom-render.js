
import React           from 'react'
import ReactDOM        from 'react-dom'
import PureRenderMixin from 'react/lib/ReactComponentWithPureRenderMixin'

const Traverse = React.createClass({
    mixins: [PureRenderMixin],
    render(){
        const props = this.props
        const type = props.typeStr.toLowerCase() === props.typeStr ?
            props.typeStr : window[props.typeStr] || never()
        const childKeys = !props.incoming ? null : props.incoming["chl"]
        const childElements = !childKeys ? null : childKeys.map(key => {
            const typeStr = key.substring(key.lastIndexOf(":")+1)
            const incoming = props.incoming[key]
            return React.createElement(Traverse, {key, typeStr, incoming})
        })
        const attributes = !props.incoming ? null : props.incoming["at"] ||
            (props.incoming["chl"] ? null : props.incoming)
        return React.createElement(type, attributes, childElements)
    }
})

const RootComponent = React.createClass({
    getInitialState(){ return { typeStr: "div" } },
    render(){ return React.createElement(Traverse,this.state) }
})

export function typePos(key){ return key.lastIndexOf(":") + 1 }

export function render(parentElement){
    const rootNativeElement = document.createElement("div")
    parentElement.appendChild(rootNativeElement)
    const rootVirtualElement = React.createElement(RootComponent,null)
    return ReactDOM.render(rootVirtualElement, rootNativeElement)
}
