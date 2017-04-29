(ns pinaclj-rich-text-editor.character
  (:require [clojure.zip :as zip]))

(defn handle [{loc :doc-loc [_ child-index text-offset] :selection-focus :as state} c]
  (assoc state :doc-loc
         (if child-index
           (zip/edit loc #(str (subs % 0 text-offset) c (subs % text-offset)))
           (zip/down (zip/insert-child loc (str c))))))
