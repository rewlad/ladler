
export default function FeedbackVDomEvent(feedback){
    const ctxToPath =
        ctx => !ctx ? "" : ctxToPath(ctx.parent) + (ctx.key ? "/"+ctx.key : "")
    const onChange = ctx => event => if(ctx.value === "send") feedback.send({
        "X-r-action": "change",
        "X-r-vdom-path": ctxToPath(ctx),
        "X-r-vdom-value": event.target.value
    })
    const transforms = {onChange}
    return ({transforms})
}
