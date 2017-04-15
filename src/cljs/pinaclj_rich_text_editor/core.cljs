(ns pinaclj-rich-text-editor.core
  (:require [pinaclj-rich-text-editor.render :as render]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(def doc (atom []))
(def bold (atom false))
(def node-key (atom 0))

(defn- next-key []
  (let [next-key @node-key]
    (swap! node-key inc)
    (str next-key)))

(defn- insert-into-string [s offset c]
  (str (subs s 0 offset) c (subs s offset)))

(defn- insert-character [loc offset c]
  (let [[tag attrs text] (zip/node loc)]
    (if @bold
      (zip/replace loc [tag attrs [:b (insert-into-string (or text "") offset c)]])
      (zip/replace loc [tag attrs (insert-into-string (or text "") offset c)]))))

(defn- attr [[tag attrs-or-child] k]
  (when (map? attrs-or-child)
    (get attrs-or-child k)))

(defn- matches-key? [node-key node]
  (= node-key (attr node :key)))

(defn- zip-to-node [doc node-key]
  (zipper/find-loc doc (partial matches-key? node-key)))

(defn- ->key [node]
  (.getAttribute node "key"))

(defn- get-selection-focus []
  (let [selection (.getSelection js/document)
        focus-node (.-focusNode selection)]
    [(->key (.-parentNode focus-node)) (.-focusOffset selection)]))

(defn- handle-keypress [e]
  (let [[focus-node-key focus-offset] (get-selection-focus)
        focus-loc (or (zip-to-node @doc focus-node-key) @doc)]
    (reset! doc (zipper/root-loc (insert-character focus-loc focus-offset (char (.-charCode e)))))))

(defn- handle-keydown [e]
  (when (.-metaKey e)
    (reset! bold true)
    (.preventDefault e)))

(defn- select-first-text-node [root]
  (let [selection (.getSelection js/document)
        rng (.createRange js/document)]
    (.setStart rng (.-firstChild (.-firstChild root)) 0)
    (.collapse rng true)
    (.addRange selection rng)))

(defn attach-editor [root new-doc]
  (reset! bold false)
  (reset! node-key 0)
  (reset! doc (zipper/->zip new-doc))
  (render/render (zip/node @doc) root)
  (select-first-text-node root)
  (.addEventListener js/document "keydown" handle-keydown)
  (.addEventListener js/document "keypress" handle-keypress))

(defn render [node]
  (render/render (zip/root @doc) node))

(enable-console-print!)
