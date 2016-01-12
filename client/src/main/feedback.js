
var loadKeyState, connectionKeyState;

function never(){ throw new Exception() }
function pong(){
    send({"X-r-action": "pong"})
    console.log("pong")
}
function sessionKey(orDo){ return sessionStorage.getItem("sessionKey") || orDo() }
function getLoadKey(orDo){ return loadKeyState || orDo() }
function loadKeyForSession(){ return "loadKeyForSession-" + sessionKey(never) }
function getConnectionKey(orDo){ return connectionKeyState || orDo() }
function connect(data) {
    console.log("conn: "+data)
    connectionKeyState = data
    sessionKey(() => sessionStorage.setItem("sessionKey", getConnectionKey(never)))
    getLoadKey(() => { loadKeyState = getConnectionKey(never) })
    localStorage.setItem(loadKeyForSession(), getLoadKey(never))
    pong()
}
function ping(data) {
    console.log("ping: "+data)
    if(localStorage.getItem(loadKeyForSession()) !== getLoadKey(never)) { // tab was refreshed/duplicated
        sessionStorage.clear()
        location.reload()
    } else if(getConnectionKey(never) === data) pong() // was not reconnected
}

export const receivers = {connect,ping}
export function send(headers){
    headers["X-r-connection"] = getConnectionKey(never)
    headers["X-r-session"] = sessionKey(never)
    fetch("/connection",{method:"post",headers})
}