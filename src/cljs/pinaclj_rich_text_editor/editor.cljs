(ns pinaclj-rich-text-editor.editor
  (:require [pinaclj-rich-text-editor.hiccup :as hiccup]
            [pinaclj-rich-text-editor.selection :as selection]
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

(defn- in-paragraph? [loc]
  (some (partial hiccup/matches-tag? :p) (cons (zip/node loc) (zip/path loc))))

(defn- ensure-in-paragraph [loc focus]
  (if-not (in-paragraph? loc)
    (-> loc
        (zip/insert-child [:p {}])
        zip/down
        (zip/insert-child ""))
    loc))

(defn insert-into-loc [{loc :doc-loc [_ child-index text-offset :as focus] :selection-focus} c]
  {:selection-focus focus
   :doc-loc (-> loc
                (ensure-in-paragraph focus)
                (insert-character child-index text-offset c)
                )})

(defn toggle-bold []
  (reset! bold true))

(defn reset []
  (reset! bold false))


