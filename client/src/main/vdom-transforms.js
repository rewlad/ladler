
export default function FeedbackVDomEvent(feedback){
    const ctxToPath =
        ctx => !ctx ? "" : ctxToPath(ctx.parent) + (ctx.key ? "/"+ctx.key : "")
    const onChange = ctx => function(event){ console.log("ThIs",this);  ctx.value === "send" ? feedback.send({
        "X-r-action": "change",
        "X-r-vdom-path": ctxToPath(ctx),
        "X-r-vdom-value-base64": btoa(unescape(encodeURIComponent(event.target.value)))
    }) : null }
    const onClick = ctx => function(event){ ctx.value === "send" ? feedback.send({
        "X-r-action": "click",
        "X-r-vdom-path": ctxToPath(ctx)
    }) : null }
    const transforms = {onChange,onClick}
    return ({transforms})
}
