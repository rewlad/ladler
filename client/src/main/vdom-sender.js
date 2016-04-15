
export default function VDomSender(feedback){ // todo: may be we need a queue to be sure server will receive messages in right order
    const ctxToPath =
        ctx => !ctx ? "" : ctxToPath(ctx.parent) + (ctx.key ? "/"+ctx.key : "")
    const send = (ctx, msg) => {
        msg["X-r-vdom-path"] = ctxToPath(ctx)	
        feedback.send(msg)
    }
    return ({send})
}