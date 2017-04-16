(ns pinaclj-rich-text-editor.editor
  (:require [clojure.zip :as zip]))

(def bold (atom false))

(defn- insert-into-string [s offset c]
  (str (subs s 0 offset) c (subs s offset)))

(defn insert-character [loc offset c]
  (let [[tag attrs text] (zip/node loc)]
    (if @bold
      (zip/replace loc [tag attrs [:b (insert-into-string (or text "") offset c)]])
      (zip/replace loc [tag attrs (insert-into-string (or text "") offset c)]))))

(defn- matches-tag? [tag node]
  (and (vector? node) (= tag (first node))))

(defn ensure-in-paragraph [loc]
  (if-not (some #(when (matches-tag? :p %) %) (cons (zip/node loc) (zip/path loc)))
    (-> loc (zip/insert-child [:p]) zip/down (zip/insert-child ""))
    loc))

(defn toggle-bold []
  (reset! bold true)
  )

(defn reset []
  (reset! bold false))


