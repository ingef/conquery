function createDragEvent(eventName, options) {
   var event = document.createEvent('HTMLEvents');
   event.initEvent('DragEvent', true, true);

   var screenX = window.screenX + options.clientX;
   var screenY = window.screenY + options.clientY;
   var clientX = options.clientX;
   var clientY = options.clientY;
   var dataTransfer = {
       data: options.dragData == null ? {} : options.dragData,
       setData: function(eventName, val){
           if (typeof val === 'string') {
               this.data[eventName] = val;
           }
       },
       getData: function(eventName){
           return this.data[eventName];
       },
       clearData: function(){
           return this.data = {};
       },
       setDragImage: function(dragElement, x, y) {}
   };
   var eventInitialized=false;
   if (event != null && event.initDragEvent) {
       try {
           event.initDragEvent(eventName, true, true, window, 0, screenX, screenY, clientX, clientY, false, false, false, false, 0, null, dataTransfer);
           event.initialized=true;
       } catch(err) {
           // no-op
       }
   }
   if (!eventInitialized) {
       event = document.createEvent("CustomEvent");
       event.initCustomEvent(eventName, true, true, null);
       event.view = window;
       event.detail = 0;
       event.screenX = screenX;
       event.screenY = screenY;
       event.clientX = clientX;
       event.clientY = clientY;
       event.ctrlKey = false;
       event.altKey = false;
       event.shiftKey = false;
       event.metaKey = false;
       event.button = 0;
       event.relatedTarget = null;
       event.dataTransfer = dataTransfer;
   }
   return event;
}
function createMouseEvent(eventName, options) {
   var event = document.createEvent("MouseEvent");
   var screenX = window.screenX + options.clientX;
   var screenY = window.screenY + options.clientY;
   var clientX = options.clientX;
   var clientY = options.clientY;
   if (event != null && event.initMouseEvent) {
       event.initMouseEvent(eventName, true, true, window, 0, screenX, screenY, clientX, clientY, false, false, false, false, 0, null);
   } else {
       event = document.createEvent("CustomEvent");
       event.initCustomEvent(eventName, true, true, null);
       event.view = window;
       event.detail = 0;
       event.screenX = screenX;
       event.screenY = screenY;
       event.clientX = clientX;
       event.clientY = clientY;
       event.ctrlKey = false;
       event.altKey = false;
       event.shiftKey = false;
       event.metaKey = false;
       event.button = 0;
       event.relatedTarget = null;
   }
   return event;
}
function dispatchEvent(webElement, eventName, event) {
   if (webElement.dispatchEvent) {
       webElement.dispatchEvent(event);
   } else if (webElement.fireEvent) {
       webElement.fireEvent("on"+eventName, event);
   }
}
function simulateEventCall(element, eventName, dragStartEvent, options) {
   var event = null;
   if (eventName.indexOf("mouse") > -1) {
       event = createMouseEvent(eventName, options);
   } else {
       event = createDragEvent(eventName, options);
   }
   if (dragStartEvent != null) {
       event.dataTransfer = dragStartEvent.dataTransfer;
   }
   dispatchEvent(element, eventName, event);
   return event;
}