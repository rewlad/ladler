
import VDom          from "../main/vdom"
import VDomSender    from "../main/vdom-sender"
import Transforms    from "../main/vdom-transforms"
import InputChanges  from "../main/input-changes"
import DiffPrepare   from "../main/diff-prepare"
import GridWatcher   from "../main/grid-watcher"

export default function VDomMix(feedback, componentClasses){
    const sender = VDomSender(feedback)
    const vDom = VDom(document.body)
    vDom.transformBy(InputChanges(sender, vDom, DiffPrepare))
    vDom.transformBy(Transforms(sender))
    vDom.transformBy(GridWatcher(vDom, DiffPrepare))
    return vDom
}
