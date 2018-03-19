function simulateHTML5DragAndDrop(dragFrom, dragTo, dragFromX, dragFromY, dragToX, dragToY) {
   var mouseDownEvent = simulateEventCall(dragFrom, "mousedown", null, {clientX: dragFromX, clientY: dragFromY});
   var dragStartEvent = simulateEventCall(dragFrom, "dragstart", null, {clientX: dragFromX, clientY: dragFromY});
   var dragEnterEvent = simulateEventCall(dragTo,   "dragenter", dragStartEvent, {clientX: dragToX, clientY: dragToY});
   var dragOverEvent  = simulateEventCall(dragTo,   "dragover",  dragStartEvent, {clientX: dragToX, clientY: dragToY});
   var dropEvent      = simulateEventCall(dragTo,   "drop",      dragStartEvent, {clientX: dragToX, clientY: dragToY});
   var dragEndEvent   = simulateEventCall(dragFrom, "dragend",   dragStartEvent, {clientX: dragToX, clientY: dragToY});
}
simulateHTML5DragAndDrop(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5]);