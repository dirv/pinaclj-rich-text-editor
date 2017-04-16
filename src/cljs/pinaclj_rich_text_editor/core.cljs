(ns pinaclj-rich-text-editor.core
  (:require [pinaclj-rich-text-editor.dom :as dom]
            [pinaclj-rich-text-editor.editor :as editor]
            [pinaclj-rich-text-editor.hiccup :as hiccup]
            [pinaclj-rich-text-editor.render :as render]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(def doc-loc (atom nil))
(def node-key (atom 0))

(defn- ->key [node]
  (.getAttribute node "key"))

(defn- next-key []
  (let [next-key @node-key]
    (swap! node-key inc)
    (str next-key)))

(defn- text-node? [node]
  (= js/Node.TEXT_NODE (.-nodeType node)))

(defn- get-selection-focus []
  (let [selection (.getSelection js/document)
        focus-node (.-focusNode selection)]
    (if (and focus-node (text-node? focus-node))
      [(->key (.-parentNode focus-node)) (.-focusOffset selection)]
      [nil (.-focusOffset selection)])))

(defn- handle-keypress [e]
  (let [[focus-node-key focus-offset] (get-selection-focus)]
    (swap! doc-loc editor/insert-into-loc focus-node-key focus-offset (char (.-charCode e)))))

(defn- handle-keydown [e]
  (when (.-metaKey e)
    (editor/toggle-bold)
    (.preventDefault e)))

(defn- select-first-node [root]
  (let [selection (.getSelection js/document)
        rng (.createRange js/document)
        first-text-node (dom/find-node text-node? root)]
    (when first-text-node
      (.setStart rng first-text-node 0)
      (.collapse rng)
      (.addRange selection rng))))

(defn- assign-key-to-node [node]
  (if (vector? node) (hiccup/insert-attr node :key (next-key)) node))

(defn- assign-keys [loc]
  (zipper/map-loc loc assign-key-to-node))

(defn- render-into [root]
  (render/render-all (zip/children (zipper/root-loc @doc-loc)) root))

(defn attach-editor [root & children]
  (editor/reset)
  (reset! doc-loc (-> (apply vector :root children) zipper/->zip assign-keys))
  (reset! node-key 0)
  (render-into root)
  (select-first-node root)
  (.addEventListener js/document "keydown" handle-keydown)
  (.addEventListener js/document "keypress" handle-keypress)
  (.addEventListener js/document "keypress" (fn [e] (render-into root))))

(enable-console-print!)
