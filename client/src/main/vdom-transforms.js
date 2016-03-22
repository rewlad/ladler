
export default function Transforms(sender){
    const send = ctx => event => sender.send(ctx, { "X-r-action": "click" })
    const onClick = ({send})
    const transforms = ({onClick})
    return ({transforms})
}
