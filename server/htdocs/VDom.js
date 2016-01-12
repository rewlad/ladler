"use strict";

function VDom(rootNativeElement){
    const Traverse = React.createClass({
        mixins: [React.addons.PureRenderMixin],
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
    const rootVirtualElement = React.createElement(RootComponent,null)
    const rootComponent = ReactDOM.render(rootVirtualElement, rootNativeElement)
    function showDiff(data){
        const diff = JSON.parse(data)
        const incoming = React.addons.update(rootComponent.state.incoming||{}, diff)
        rootComponent.setState({incoming})
    }
    return ({showDiff})
}
