(ns pinaclj-rich-text-editor.core
  (:require [pinaclj-rich-text-editor.render :as render]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(def doc (atom []))
(def bold (atom false))

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
    (if (= (.-nodeType focus-node) js/Node.TEXT_NODE)
      [(->key (.-parentNode focus-node)) (.-focusOffset selection)]
      [(->key focus-node) (.-focusOffset selection)])))

(defn- matches-tag? [tag node]
  (and (vector? node) (= tag (first node))))

(defn- ensure-in-paragraph [loc]
  (if-not (some #(when (matches-tag? :p %) %) (cons (zip/node loc) (zip/path loc)))
    (-> loc (zip/insert-child [:p]) zip/down (zip/insert-child ""))
    loc))

(defn- handle-keypress [e]
  (let [[focus-node-key focus-offset] (get-selection-focus)
        focus-loc (zip-to-node @doc focus-node-key)]
    (reset! doc (-> focus-loc
                    ensure-in-paragraph
                    (insert-character focus-offset (char (.-charCode e)))
                    zipper/root-loc))))

(defn- handle-keydown [e]
  (when (.-metaKey e)
    (reset! bold true)
    (.preventDefault e)))

(defn- select-first-text-node [root]
  (let [selection (.getSelection js/document)
        rng (.createRange js/document)]
    (.setStart rng root 0)
    (.collapse rng true)
    (.addRange selection rng)))

(defn attach-editor [root new-doc]
  (reset! bold false)
  (reset! doc (zipper/->zip new-doc))
  (render/render (zip/node @doc) root)
  (select-first-text-node root)
  (.addEventListener js/document "keydown" handle-keydown)
  (.addEventListener js/document "keypress" handle-keypress))

(defn render [node]
  (render/render (zip/root @doc) node))

(enable-console-print!)
