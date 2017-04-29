(ns pinaclj-rich-text-editor.paragraph
  (:require [pinaclj-rich-text-editor.hiccup :as hiccup]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(defn- in-paragraph? [loc]
  (some #{:p} (zipper/tag-path loc)))

(defn handle [{loc :doc-loc :as state} e]
  (if-not (in-paragraph? loc)
    (assoc state :doc-loc (-> loc
                              (zip/insert-child [:p {}])
                              zip/down
                              (zip/insert-child "")
                              zip/down))
    state))
