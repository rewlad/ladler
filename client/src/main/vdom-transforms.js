
export default function Transforms(sender){
    const send = ctx => event => {
        sender.send(ctx, { "X-r-action": "click" })
        event.stopPropagation()
    }
     const sendThen = ctx => event => {
            sender.send(ctx, { "X-r-action": "click" })

        }
    const onClick = ({send,sendThen})

    const onResize={
    	"feedback()":ctx=>event=>{
    	    sender.send(ctx,{"X-r-action":"resize","X-r-width":event.width})
    	}
    }
    const onCheck={
        "send":ctx=>event=>{
            sender.send(ctx,{"X-r-action":"check","X-r-checked":event.target.checked})
        }
    }
    const transforms = ({onClick,onResize,onCheck})
    return ({transforms})
}
