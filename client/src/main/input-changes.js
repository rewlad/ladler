
function wrap(k,v){
    const res = {}
    res[k] = v
    return res
}

export default function InputChanges(sender){
    var value, eventCtx
    
    const getDiff = 
        () => eventCtx ? ctxToDiff(eventCtx, { value: { $set: value } }) : null
    const ctxToDiff = 
        (ctx,res) => ctx ? ctxToDiff(ctx.parent, ctx.key ? wrap(ctx.key, res) : res) : res
    
    const send = () => !eventCtx ? null : sender.send(eventCtx, {
        "X-r-action": "change",
        "X-r-vdom-value-base64": btoa(unescape(encodeURIComponent(value)))
    })
    
    const update = 
        ctx => ctx.updateMergedState ? ctx.updateMergedState() : update(ctx.parent)
    const set = (ctx,v) => {
        eventCtx = ctx
        value = v
        update(eventCtx)
    }
    
    return {getDiff,set,send}
}