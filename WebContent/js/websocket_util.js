function close_connection(socket){
    socket.onclose = function () {}; // disable onclose handler first
	socket.close();
}
function buildSocketUrl(socket, queryStr) {
    var l = window.location;
    var path = l.pathname;
    path = path.substring(0, path.lastIndexOf('/')+1);
    path = ((l.protocol === "https:") ? "wss://" : "ws://") + l.host + path + socket;

    if(queryStr){
        path += "?"+queryStr;
    }

    return path;
}