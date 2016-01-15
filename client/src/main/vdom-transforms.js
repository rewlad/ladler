
export default function Transforms(componentClasses, sender, inputChanges){
    const tp = ctx => componentClasses[ctx.value] || ctx.value
    const key = ctx => ctx.key || key(ctx.parent)
    
    const onChange = ctx => event => {
        inputChanges.set(ctx, event.target.value)
        if(ctx.value === "send") inputChanges.send()
    }
    const onBlur = ctx => event => {
        if(ctx.value === "send") inputChanges.send()
        inputChanges.set(null,null)
    }
    
    const onClick = ctx => event => ctx.value === "send" ? sender.send(ctx, {
        "X-r-action": "click"
    }) : null 
    
    const transforms = {tp,key,onChange,onBlur,onClick}
    return ({transforms})
}
