"use strict";

function SSEConnection(address,handlers){
    var eventSource
    var closedCount = 0

    function isStateClosed(v){ return v === 2 }
    function checkReconnect(){
        if(eventSource){
            closedCount = isStateClosed(eventSource.readyState) ? closedCount + 1 : 0
            if(closedCount > 0) console.log("closedCount: "+closedCount)
            if(closedCount > 5){
                eventSource.close();
                eventSource = null;
            }
        }
        if(!eventSource){
            console.log("new EventSource")
            eventSource = new EventSource(address);
            handlers.forEach(handlerMap => {
                for(var k in handlerMap)
                    eventSource.addEventListener(k, handlerMap[k])
            })
        }
    }

    setInterval(checkReconnect,1000);
    checkReconnect();
}
