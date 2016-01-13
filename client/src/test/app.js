
"use strict";

import MergeHandlers from "../main/handlers"
import SSEConnection from "../main/sse-connection"
import Feedback      from "../main/feedback"
import VDom          from "../main/vdom-render"
import Update        from "../main/vdom-update"

const componentClasses = {}
const transformers = MergeHandlers([])
const vdom = VDom(document.body, componentClasses, transformers)
const receivers = [Feedback.receivers, Update(vdom)]
    
SSEConnection("http://localhost:5556/sse", receivers, 5)
