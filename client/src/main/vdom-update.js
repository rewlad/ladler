
import update from 'react/lib/update'

import VDom   from "./vdom-render"

const valueHandlers = {
    "default": (obj,key,value,path) => {
        const typePos = key.lastIndexOf(":") + 1
        if(typePos > 0) setup(value, [key,path])
    },
    "$set": (obj,key,value,path) => setup(value,path),
    "sendChange": (obj,key,value,path) => {
        obj["at"].onChange = event => send(path, event.target.value)
    }
}

function setup(obj, path) {
    Object.keys(obj).forEach(key => 
        (valueHandlers[key]||valueHandlers["default"])(obj,key,obj[key],path)
    )
}

export default function VDomUpdate(rootComponent){
    function showDiff(data){
        const diff = JSON.parse(data)
        const incoming = update(rootComponent.state.incoming||{}, diff)
        rootComponent.setState({incoming})
    }
    return ({showDiff})
}