(ns pinaclj-rich-text-editor.key-strokes)

(def mappings
  {(.charCodeAt \B) :B})

(defn- key-code [key-event]
  (get mappings (.-keyCode key-event)))

(defn ->key-stroke [e]
  (if (.-metaKey e)
    #{:meta (key-code e)}
    #{(key-code e)}))

