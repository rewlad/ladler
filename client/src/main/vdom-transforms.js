
const wrap = (k,v) => {
    const res = {}
    res[k] = v
    return res
}

export default function FeedbackVDomEvent(componentClasses, feedback){
    
    const tp = ctx => {
        if(!ctx.key) return tp(ctx.parent)
        const typeStr = ctx.key.substring(ctx.key.lastIndexOf(":")+1)
        return componentClasses[typeStr] || typeStr
    }
    const key = ctx => ctx.key || key(ctx.parent)
    
    const ctxToPath =
        ctx => !ctx ? "" : ctxToPath(ctx.parent) + (ctx.key ? "/"+ctx.key : "")
    const ctxToDiff = 
        (ctx,res) => ctx.setTempDiff ? ctx.setTempDiff(res) : ctxToDiff(ctx.parent, ctx.key ? wrap(ctx.key, res) : res)
    const onChange = ctx => event => { 
        console.log("ThIs",this)
        const value = event.target.value
        ctxToDiff(ctx.parent, { value: { $set: value } })
        
        ctx.value === "send" ? feedback.send({
            "X-r-action": "change",
            "X-r-vdom-path": ctxToPath(ctx),
            "X-r-vdom-value-base64": btoa(unescape(encodeURIComponent(value)))
        }) : null
    }
    const onClick = ctx => event => ctx.value === "send" ? feedback.send({
        "X-r-action": "click",
        "X-r-vdom-path": ctxToPath(ctx)
    }) : null 
    
    const transforms = {tp,key,onChange,onClick}
    return ({transforms})
}
