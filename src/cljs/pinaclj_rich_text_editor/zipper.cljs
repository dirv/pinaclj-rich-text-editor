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
  (let [mapped-loc (zip/edit z map-fn)
        next-loc (zip/next mapped-loc)]
    (if (zip/end? next-loc)
      mapped-loc
      (recur next-loc map-fn))))

(defn ->focus [loc text-position])

(defn- distance-between [parent-loc loc]
  (count (take-while #(not= parent-loc %) (iterate zip/up loc))))

(defn- up-times [n loc]
  (nth (iterate zip/up loc) n))

(defn- remove-left-siblings [loc]
  (let [parent (zip/up loc)]
    (if (zip/lefts loc)
    (let [children (zip/children parent)]
      (zip/replace parent (make-node (zip/node parent) (subvec children (count (zip/lefts loc))))))
    parent)))

(defn- remove-right-siblings [loc]
  (let [parent (zip/up loc)]
    (if (zip/rights loc)
      (let [children (zip/children parent)]
        (zip/replace parent (make-node (zip/node parent) (subvec children 0 (inc (count (zip/lefts loc)))))))
      parent)))

(defn split-node [parent-loc text-node-loc position]
  (let [distance (distance-between parent-loc text-node-loc)
        left (remove-right-siblings (up-times (dec distance) (zip/edit text-node-loc subs 0 position)))
        right (remove-left-siblings (up-times (dec distance) (zip/edit text-node-loc subs position)))]
    (mapv zip/node [left right])))


