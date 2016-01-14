
export default function FeedbackVDomEvent(feedback){
    const ctxToPath =
        ctx => !ctx ? "" : ctxToPath(ctx.parent) + (ctx.key ? "/"+ctx.key : "")
    const onChange = ctx => event => if(ctx.value === "send") feedback.send({
        "X-r-action": "change",
        "X-r-vdom-path": ctxToPath(ctx),
        "X-r-vdom-value": event.target.value
    })
    const onClick = ctx => event => if(ctx.value === "send") feedback.send({
        "X-r-action": "click",
        "X-r-vdom-path": ctxToPath(ctx)
    })
    const transforms = {onChange,onClick}
    return ({transforms})
}
