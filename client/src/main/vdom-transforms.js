
export default function Transforms(componentClasses, sender, inputChanges){
    const tp = ctx => componentClasses[ctx.value] || ctx.value
    const key = ctx => !ctx ? "" : ctx.key || key(ctx.parent)
    const update = 
        ctx => ctx.updateMergedState ? ctx.updateMergedState() : update(ctx.parent)
    const onChange = ctx => event => {
        inputChanges.set(ctx, event.target.value)
        update(ctx)
        if(ctx.value === "send") inputChanges.send()
    }
    const onBlur = ctx => event => {
        if(ctx.value === "send") inputChanges.send()
        inputChanges.set(null, null)
        update(ctx)
    }
    
    const onClick = ctx => event => ctx.value === "send" ? sender.send(ctx, {
        "X-r-action": "click"
    }) : null 
    
    const transforms = {tp,key,onChange,onBlur,onClick}
    return ({transforms})
}
