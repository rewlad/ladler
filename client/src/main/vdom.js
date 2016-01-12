
import React           from 'react'
import ReactDOM        from 'react-dom'
import PureRenderMixin from 'react/lib/ReactComponentWithPureRenderMixin'
import update          from 'react/lib/update'

const Traverse = React.createClass({
    mixins: [PureRenderMixin],
    render: function() {
        const props = this.props
        const type = props.typeStr.toLowerCase() === props.typeStr ?
            props.typeStr : window[props.typeStr] || never()
        const childKeys = !props.incoming ? null : props.incoming["chl"]
        const childElements = !childKeys ? null : childKeys.map(function(key){
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
    getInitialState: function() { return { typeStr: "div" } },
    render: function() { return React.createElement(Traverse,this.state) }
})

export default function VDom(parentElement){
    const rootNativeElement = document.createElement("div")
    parentElement.appendChild(rootNativeElement)
    const rootVirtualElement = React.createElement(RootComponent,null)
    const rootComponent = ReactDOM.render(rootVirtualElement, rootNativeElement)
    function showDiff(data){
        const diff = JSON.parse(data)
        const incoming = update(rootComponent.state.incoming||{}, diff)
        rootComponent.setState({incoming})
    }
    return ({showDiff})
}
