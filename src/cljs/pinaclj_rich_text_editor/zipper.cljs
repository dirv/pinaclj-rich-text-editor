(ns pinaclj-rich-text-editor.zipper
  (:require [clojure.zip :as zip]
            [pinaclj-rich-text-editor.hiccup :as hiccup]))

(def ->zip
  (partial zip/zipper vector? hiccup/children hiccup/replace-children))

(defn- root-loc [z]
  (last (take-while #(not (nil? %)) (iterate zip/up z))))

(defn- dfs [z]
  (take-while #(not (zip/end? %)) (iterate zip/next (root-loc z))))

(defn find-loc [z predicate]
  (some #(when (predicate (zip/node %)) %) (dfs z)))

(defn map-loc [map-fn z]
  (let [mapped-loc (zip/edit z map-fn)
        next-loc (zip/next mapped-loc)]
    (if (zip/end? next-loc)
      mapped-loc
      (recur map-fn next-loc))))

(defn ->focus [loc text-position])

(defn- distance-between [parent-loc loc]
  (count (take-while #(not= parent-loc %) (iterate zip/up loc))))

(defn- up-times [n loc]
  (nth (iterate zip/up loc) n))

(defn- remove-left-siblings [loc]
  (let [parent (zip/up loc)]
    (if (zip/lefts loc)
    (let [children (zip/children parent)]
      (zip/replace parent (hiccup/replace-children (zip/node parent) (subvec children (count (zip/lefts loc))))))
    parent)))

(defn- remove-right-siblings [loc]
  (let [parent (zip/up loc)]
    (if (zip/rights loc)
      (let [children (zip/children parent)]
        (zip/replace parent (hiccup/replace-children (zip/node parent) (subvec children 0 (inc (count (zip/lefts loc)))))))
      parent)))

(defn- split-text-node [text-node-loc position]
  [(zip/edit text-node-loc subs 0 position) (zip/edit text-node-loc subs position)])

(defn- remove-siblings [text-node-loc distance remove-fn]
  (if (not= "" (zip/node text-node-loc))
    (->> text-node-loc
         (up-times distance)
        remove-fn
        zip/node)))

(defn split-node [parent-loc text-node-loc position]
  (let [distance (dec (distance-between parent-loc text-node-loc))
        [left-text right-text] (split-text-node text-node-loc position)
        left (remove-siblings left-text distance remove-right-siblings)
        right (remove-siblings right-text distance remove-left-siblings)]
    [left right]))

