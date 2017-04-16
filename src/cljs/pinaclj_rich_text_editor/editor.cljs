(ns pinaclj-rich-text-editor.editor
  (:require [pinaclj-rich-text-editor.hiccup :as hiccup]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(def bold (atom false))

(defn- insert-into-string [s offset c]
  (str (subs s 0 offset) c (subs s offset)))

(defn- insert-character [loc offset c]
  (let [[tag attrs text] (zip/node loc)]
    (if @bold
      (zip/replace loc [tag attrs [:b (insert-into-string (or text "") offset c)]])
      (zip/replace loc [tag attrs (insert-into-string (or text "") offset c)]))))

(defn- matches-tag? [tag node]
  (and (vector? node) (= tag (first node))))


(defn- ensure-in-paragraph [loc]
    (or (zipper/find-loc loc (partial matches-tag? :p))
      (-> loc (zip/insert-child [:p {}]) zip/down (zip/insert-child ""))))

(defn insert-into-loc [doc node-key focus-offset c]
  (-> (if node-key
        (zipper/find-loc doc (partial hiccup/matches-attr? :key node-key))
        (ensure-in-paragraph doc))
      (insert-character focus-offset c)))

(defn toggle-bold []
  (reset! bold true))

(defn reset []
  (reset! bold false))


