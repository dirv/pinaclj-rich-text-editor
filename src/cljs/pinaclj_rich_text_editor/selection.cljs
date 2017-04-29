(ns pinaclj-rich-text-editor.selection
  (:require
    [pinaclj-rich-text-editor.dom :as dom]
    [pinaclj-rich-text-editor.hiccup :as hiccup]
    [pinaclj-rich-text-editor.zipper :as zipper]
    [clojure.zip :as zip]))

(defn- ->caret [text-node text-position]
  (let [parent (.-parentNode text-node)]
    [(dom/attr :key parent)
     (dom/child-index parent text-node)
     text-position]))

(defn- get-selection-focus []
  (let [selection (.getSelection js/document)
        focus-node (.-focusNode selection)]
    (when (and focus-node (dom/text-node? focus-node))
      (->caret focus-node (.-focusOffset selection)))))

(defn- node-key-matcher [node-key]
  (partial hiccup/matches-attr? :key node-key))

(defn move-loc-to-focus [loc [node-key current-text-node _]]
  (let [parent-node (or (zipper/find-loc loc (node-key-matcher node-key)) loc)]
    (if-not (nil? current-text-node)
      (nth (iterate zip/next parent-node) (inc current-text-node))
      parent-node)))

(defn- ->key [loc]
  (hiccup/attr (zip/node loc) :key))

(defn- focus-at-first-para [loc]
  [(->key (or (zipper/find-loc loc (partial hiccup/matches-tag? :p))
              (zipper/root-loc loc))) 0 0])

(defn on-focus-changed [{doc-loc :doc-loc :as state}]
  (let [new-focus (or (get-selection-focus) (focus-at-first-para doc-loc))]
    (assoc state
           :selection-focus new-focus
           :doc-loc (move-loc-to-focus doc-loc new-focus))))

(defn select-first-node [root]
  (let [selection (.getSelection js/document)
        rng (.createRange js/document)
        first-text-node (dom/find-node dom/text-node? root)]
    (when first-text-node
      (.setStart rng first-text-node 0)
      (.collapse rng)
      (.addRange selection rng))))
