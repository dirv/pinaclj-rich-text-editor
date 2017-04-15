(ns pinaclj-rich-text-editor.core
  (:require [incremental-dom :as idom]))

(defn- render-element [[tag content]]
  (idom/elementOpen (name tag))
  (idom/text content)
  (idom/elementClose (name tag)))

(def doc (atom []))

(defn render [node]
  (idom/patch node #(render-element %) @doc))

(defn- insert-character [c doc]
  [:p (str (nth doc 1) c)])

(defn- handle-keypress [e]
  (swap! doc
         (partial insert-character (char (.-charCode e)))))

(defn attach-editor [root]
  (.addEventListener js/document "keypress" handle-keypress))

(defn load-doc [new-doc]
  (reset! doc new-doc))

(enable-console-print!)
