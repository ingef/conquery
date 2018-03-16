function simulateEvent(element, eventName, clientX, clientY, dragData) {
   return simulateEventCall(element, eventName, null, {clientX: clientX, clientY: clientY, dragData: dragData});
}
var event = simulateEvent(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);
if (event.dataTransfer != null) {
   return event.dataTransfer.data;
}