
"use strict";

import SSEConnection from "../main/sse-connection"
import Feedback      from "../main/feedback"
import VDom          from "../main/vdom"
import FeedbackVDomEvent from "../main/vdom-transforms"

const componentClasses = {}
const transforms = FeedbackVDomEvent(Feedback).transforms
const vdom = VDom(document.body, componentClasses, transforms)
const receivers = [Feedback.receivers, vdom.receivers]
SSEConnection("http://localhost:5556/sse", receivers, 5)
