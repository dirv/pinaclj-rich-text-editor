(ns pinaclj-rich-text-editor.character
  (:require [clojure.zip :as zip]))

(defn- create-or-insert-into-string [s offset c]
  (if s
    (str (subs s 0 offset) c (subs s offset))
    (str c)))

(defn- replace-child [loc child-index replace-fn]
  (let [[tag attrs & children] (zip/node loc)
        new-child (replace-fn (nth children child-index))]
    (zip/replace loc (apply vector tag attrs (assoc (apply vector children) child-index new-child)))))

(defn handle [{loc :doc-loc [_ child-index text-offset] :selection-focus :as state} c]
  (assoc state :doc-loc (replace-child loc child-index #(create-or-insert-into-string % text-offset c))))
