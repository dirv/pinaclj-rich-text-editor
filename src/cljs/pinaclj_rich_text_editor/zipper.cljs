(ns pinaclj-rich-text-editor.zipper
  (:require [clojure.zip :as zip]))

(defn- children [[_ & [attrs-or-first-child & remaining-children :as all-children]]]
  (if (map? attrs-or-first-child) remaining-children all-children))

(defn- make-node [[tag & existing] replace-children]
  (if (map? (first existing))
    (into [tag (first existing)] replace-children)
    (into [tag] replace-children)))

(def ->zip
  (partial zip/zipper vector? children make-node))

(defn- root-loc [z]
  (last (take-while #(not (nil? %)) (iterate zip/up z))))

(defn- dfs [z]
  (take-while #(not (zip/end? %)) (iterate zip/next (root-loc z))))

(defn find-loc [z predicate]
  (some #(when (predicate (zip/node %)) %) (dfs z)))

(defn map-loc [z map-fn]
  (last (take-while #(not (zip/end? %))
                    (iterate #(-> % (zip/edit map-fn) zip/next) (root-loc z)))))

