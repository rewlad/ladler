
import VDom          from "../main/vdom"
import VDomSender    from "../main/vdom-sender"
import Transforms    from "../main/vdom-transforms"
import InputChanges  from "../main/input-changes"

export default function VDomMix(feedback, componentClasses){
    const sender = VDomSender(feedback)
    const inputChanges = InputChanges(sender)
    const transforms = Transforms(componentClasses, sender, inputChanges).transforms
    const vdom = VDom(document.body, transforms, inputChanges.getDiff)
    return vdom
}
