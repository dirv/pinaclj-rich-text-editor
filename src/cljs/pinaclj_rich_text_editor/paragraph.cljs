(ns pinaclj-rich-text-editor.paragraph
  (:require [pinaclj-rich-text-editor.hiccup :as hiccup]
            [clojure.zip :as zip]))

(defn- in-paragraph? [loc]
  (some (partial hiccup/matches-tag? :p) (cons (zip/node loc) (zip/path loc))))

(defn handle [{loc :doc-loc :as state} e]
  (if-not (in-paragraph? loc)
    (assoc state :doc-loc (-> loc
                              (zip/insert-child [:p {}])
                              zip/down
                              (zip/insert-child "")))
    state))
