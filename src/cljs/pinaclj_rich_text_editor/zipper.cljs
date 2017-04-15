(ns pinaclj-rich-text-editor.zipper
  (:require [clojure.zip :as zip]))

(defn- children [[_ & [attrs-or-first-child & remaining-children :as all-children]]]
  (if (map? attrs-or-first-child) remaining-children all-children))

(defn- make-node [node children]
  (into node children))

(def ->zip
  (partial zip/zipper vector? children make-node))

(defn- root-loc [z]
  (last (take-while #(not (nil? %)) (iterate zip/up z))))

(defn- dfs [z]
  (take-while #(not (zip/end? %)) (iterate zip/next (root-loc z))))

(defn find-loc [z predicate]
  (some #(when (predicate (zip/node %)) %) (dfs z)))

