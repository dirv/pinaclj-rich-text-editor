(ns pinaclj-rich-text-editor.core
  (:require [pinaclj-rich-text-editor.editor :as editor]
            [pinaclj-rich-text-editor.render :as render]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(def doc-loc (atom nil))

(defn- ->key [node]
  (.getAttribute node "key"))

(defn- text-node? [node]
  (= js/Node.TEXT_NODE (.-nodeType node)))

(defn- get-selection-focus []
  (let [selection (.getSelection js/document)
        focus-node (.-focusNode selection)]
    (if (= (.-nodeType focus-node) js/Node.TEXT_NODE)
      [(->key (.-parentNode focus-node)) (.-focusOffset selection)]
      [(->key focus-node) (.-focusOffset selection)])))

(defn- handle-keypress [e]
  (let [[focus-node-key focus-offset] (get-selection-focus)]
    (swap! doc-loc editor/insert-into-loc focus-node-key focus-offset (char (.-charCode e)))))

(defn- handle-keydown [e]
  (when (.-metaKey e)
    (editor/toggle-bold)
    (.preventDefault e)))

(defn- select-first-node [root]
  (let [selection (.getSelection js/document)
        rng (.createRange js/document)]
    (.setStart rng root 0)
    (.collapse rng true)
    (.addRange selection rng)))

(defn- render-into [root]
  (render/render (zip/root @doc-loc) root))

(defn attach-editor [root new-doc]
  (editor/reset)
  (reset! doc-loc (zipper/->zip new-doc))
  (render-into root)
  (select-first-node root)
  (.addEventListener js/document "keydown" handle-keydown)
  (.addEventListener js/document "keypress" handle-keypress)
  (.addEventListener js/document "keypress" (fn [e] (render-into root))))

(defn render [node]
  (render/render (zip/root @doc-loc) node))

(enable-console-print!)
