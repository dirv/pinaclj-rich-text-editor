(ns pinaclj-rich-text-editor.editor
  (:require [pinaclj-rich-text-editor.hiccup :as hiccup]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(def bold (atom false))

(defn- create-or-insert-into-string [s offset c]
  (if s
    (str (subs s 0 offset) c (subs s offset))
    (str c)))

(defn- replace-child [loc child-index replace-fn]
  (let [[tag attrs & children] (zip/node loc)
        new-child (replace-fn (nth children child-index))]
    (zip/replace loc (apply vector tag attrs (assoc (apply vector children) child-index new-child)))))

(defn- insert-character [loc child-index offset c]
  (if @bold
    (replace-child loc child-index #(vector :b (create-or-insert-into-string % offset c)))
    (replace-child loc child-index #(create-or-insert-into-string % offset c))))

(defn- matches-tag? [tag node]
  (and (vector? node) (= tag (first node))))

(defn- ensure-in-paragraph [loc]
    (or (zipper/find-loc loc (partial matches-tag? :p))
      (-> loc (zip/insert-child [:p {}]) zip/down (zip/insert-child ""))))

(defn insert-into-loc [doc [focus-key child-index text-offset] c]
  (-> (if focus-key
        (zipper/find-loc doc (partial hiccup/matches-attr? :key focus-key))
        (ensure-in-paragraph doc))
      (insert-character child-index text-offset c)))

(defn toggle-bold []
  (reset! bold true))

(defn reset []
  (reset! bold false))


