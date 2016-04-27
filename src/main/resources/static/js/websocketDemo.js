//Establish the WebSocket connection and set up event handlers

if (document.location.hostname == "localhost") {
	// use http
	webSocket = new WebSocket("ws://" + location.hostname + ":" + location.port + "/action/");
} else {
	// we're on heroku - use https:
	webSocket = new WebSocket("wss://" + location.hostname + ":" + location.port + "/action/");
}


webSocket.onopen = function () {
	// don't send get game state until we're sure we're logged in.	
};


function getCookie(name) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0;i < ca.length;i++) {
	var c = ca[i];
	while (c.charAt(0)==' ') c = c.substring(1);
	if (c.indexOf(nameEQ) != -1){
		return c.substring(nameEQ.length,c.length);
		}
	}
	return null;
}
function setCookie(cookie, value) {
	var eqVal = cookie + "=" + value;
	document.cookie = eqVal;
}

function handleSetCookie(data) {
	console.log(data);
	for(i=0; i < data.cookies.length; i++) {
		if(data.cookies[i].name == "CATAN_USER_ID") {
			var cook = data.cookies[i];
			setCookie(cook.name, cook.value);
			console.log("Cookies set to :" + document.cookie);
			sendGetGameStateAction();
		}
	}
}

webSocket.onmessage = function (msg) {
    var data = JSON.parse(msg.data);
    console.log(data);

    if(data.hasOwnProperty("requestType")) {
    	switch(data.requestType) {
    	case "chat":
    		updateChat(data);
    		return;
    	case "getGameState":
    		handleGetGameState(data);
    		return;
        case "action":
            handleActionResponse(data);
            return;
        case "setCookie":
        	handleSetCookie(data);
        	return;
    	default:
    		console.log("unsupported request type");
    		return;
    	}
    } else {
    	console.log("No request type indicated for response");
    }
};

function handleActionResponse(data) {
	switch(data.action) {
	// add action handlers here!
	case  "buildSettlement":
		return handleBuildSettlement(data);
	default:
		console.log("action object with no action identifier");
	}
}

function sendGetGameStateAction() {
    var playersReq = {requestType: "getGameState"};
    webSocket.send(JSON.stringify(playersReq));
}

function sendRollDiceAction() {
    var rollDiceReq  = {requestType: "action", "action": "rollDice", "player": playerId};
    webSocket.send(JSON.stringify(rollDiceReq));
}

function sendBuildSettlementAction(intersectCoordinates) {
    var buildReq  = {requestType: "action", "action": "buildSettlement", "coordinate": intersectCoordinates, "player": 0};
    webSocket.send(JSON.stringify(buildReq));
}

function sendBuildCityAction(intersectCoordinates) {
    var buildReq  = {requestType: "action", "action" : "buildCity"};
    webSocket.send(JSON.stringify(buildReq));
}

function sendBuildRoadAction(start, end) {
    var buildReq  = {requestType: "action", "action": "buildRoad"};
    webSocket.send(JSON.stringify(buildReq));
}

function handleGetGameState(gameStateData) {
    // Set global data
    playerId = gameStateData.playerID;
    currentTurn = gameStateData.currentTurn;

    // Create players
    playersById = {};
    players = parsePlayers(gameStateData.players);
    for (var i = 0; i < players.length; i++) {
        playersById[players[i].id] = players[i];
    }

    for (var i = 0; i < 4; i++) {
        players[i].fillPlayerTab();
    }

    // Draw hand
    fillPlayerHand(gameStateData.hand);

    // Draw trade rates
    fillPlayerTradeRates(gameStateData.players[playerId].rates); // TODO: change to reflect current player

    // Create board
    board = new Board();
    board.createBoard(gameStateData.board);
    board.draw();
}

function handleBuildSettlement(response) {
}

//Send message if "Send" is clicked
id("send").addEventListener("click", function () {
    sendMessage(id("message").value);
});

//Send message if enter is pressed in the input field
id("message").addEventListener("keypress", function (e) {
    if (e.keyCode === 13) { sendMessage(e.target.value); }
});


//Send a message if it's not empty, then clear the input field
function sendMessage(message) {
    if (message !== "") {
    	var pack = {"requestType" : "chat", "message" : message};
        webSocket.send(JSON.stringify(pack));
        id("message").value = "";
    }
}

//Update the chat-panel, and the list of connected users
function updateChat(msg) {
    console.log(msg);
    if(msg.hasOwnProperty('ERROR')) {
    	alert(msg.ERROR);
    } else {
        insert("chat", msg.userMessage);
    }

}

//Helper function for inserting HTML as the first child of an element
function insert(targetId, message) {
    id(targetId).insertAdjacentHTML("afterbegin", message);
}

//Helper function for selecting element by id
function id(id) {
    return document.getElementById(id);
}
