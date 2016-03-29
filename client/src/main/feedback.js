
var loadKeyState, connectionKeyState, locationHash;

function never(){ throw new Exception() }
function pong(){
    const headers = { 
        "X-r-action": "pong", 
        "X-r-session": sessionKey(never),
        "X-r-location-hash": location.hash.substr(1)
    }
    send(headers)
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
    window.onhashchange = () => pong()
    pong()
}
function ping(data) {
    console.log("ping: "+data)
    if(localStorage.getItem(loadKeyForSession()) !== getLoadKey(never)) { // tab was refreshed/duplicated
        sessionStorage.clear()
        location.reload()
    } else if(getConnectionKey(never) === data) pong() // was not reconnected
}
function send(headers){
    headers["X-r-connection"] = getConnectionKey(never)
    fetch("/connection",{method:"post",headers})
}
function relocateHash(data) { 
    document.location.href = "#"+data 
}
export default function(){
    const receivers = {connect,ping,relocateHash}
    return ({receivers,send}) 
}