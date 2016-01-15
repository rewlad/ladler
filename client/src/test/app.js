
"use strict";

import SSEConnection from "../main/sse-connection"
import Feedback      from "../main/feedback"
import VDom          from "../main/vdom"
import VDomSender    from "../main/vdom-sender"
import Transforms    from "../main/vdom-transforms"
import InputChanges  from "../main/input-changes"

const feedback = Feedback()

const componentClasses = {}
const sender = VDomSender(feedback)
const inputChanges = InputChanges(sender)
const transforms = Transforms(componentClasses, sender, inputChanges).transforms
const vdom = VDom(document.body, transforms, inputChanges.getDiff)
    
const receivers = [feedback.receivers, vdom.receivers]
SSEConnection("http://localhost:5556/sse", receivers, 5)
