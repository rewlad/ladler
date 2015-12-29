"use strict";

function SSEConnection(address,handlers,reconnectTimeout){
    var eventSource
    var closedCount = 0

    function isStateClosed(v){ return v === 2 }
    function checkReconnect(){
        if(eventSource){
            closedCount = isStateClosed(eventSource.readyState) ? closedCount + 1 : 0
            if(closedCount > 0) console.log("closedCount: "+closedCount)
            if(closedCount > reconnectTimeout){
                eventSource.close();
                eventSource = null;
            }
        }
        if(!eventSource){
            console.log("new EventSource")
            eventSource = new EventSource(address);
            handlers.forEach(function(handlerMap){
                for(let k in handlerMap)
                    eventSource.addEventListener(k, function(event){
                        handlerMap[k](event.data)
                    })
            })
        }
    }

    setInterval(checkReconnect,1000);
    checkReconnect();
}
