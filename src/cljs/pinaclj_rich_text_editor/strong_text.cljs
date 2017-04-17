(ns pinaclj-rich-text-editor.strong-text
  (:require [clojure.zip :as zip]))

(defn initialize [state]
  (assoc state :strong false))

(defn- insert-strong [new-key node]
  (conj node [:b {:key new-key} ""]))

(defn handle [{strong :strong loc :doc-loc :as state} c]
  (if strong
    (let [new-key ((:next-key-fn state))]
      (assoc state
             :doc-loc (-> loc (zip/edit (partial insert-strong new-key)) zip/next zip/next)
             :selection-focus [new-key 0 0]))
    state))

(defn toggle [state e]
  (when (.-metaKey e)
    (assoc state :strong true)))
