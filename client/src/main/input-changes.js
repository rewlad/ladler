
export default function InputChanges(sender, vDom, DiffPrepare){
    var value, eventCtx
    const send = () => !eventCtx ? null : sender.send(eventCtx, "change", value)
    const set = (ctx,v) => {
        eventCtx = ctx
        value = v
        const diff = DiffPrepare(vDom.localState)
        diff.jump(vDom.ctxToArray(eventCtx,[]))
        diff.addIfChanged("value", value)
        diff.apply()
    }
    
    const onChange = {
        "local": ctx => event => set(ctx, event.target.value),
        "send": ctx => event => {
            set(ctx, event.target.value)
            send()
        }
    }
    const onBlur = {
        "send": ctx => event => {
            send()
            eventCtx = null
            value = null
            const diff = DiffPrepare(vDom.localState)
            diff.jump(vDom.ctxToArray(ctx,[]).slice(0,-1))
            diff.addIfChanged("at", {}) //fix if resets alien props
            diff.apply()
        },
	"blur": ctx => event =>{
		sender.send(ctx,"blur","")
	}
    }

    const transforms = {onChange,onBlur}
    return ({transforms})
}