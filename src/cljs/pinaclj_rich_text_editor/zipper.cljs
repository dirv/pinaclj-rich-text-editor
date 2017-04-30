(ns pinaclj-rich-text-editor.zipper
  (:require [clojure.zip :as zip]
            [pinaclj-rich-text-editor.hiccup :as hiccup]))

(def ->zip
  (partial zip/zipper vector? hiccup/children hiccup/replace-children))

(defn- root-loc [z]
  (last (take-while #(not (nil? %)) (iterate zip/up z))))

(defn- dfs [z]
  (take-while #(not (zip/end? %)) (iterate zip/next z)))

(defn find-loc [z predicate]
  (some #(when (predicate (zip/node %)) %) (dfs z)))

(defn map-loc [map-fn z]
  (let [mapped-loc (zip/edit z map-fn)
        next-loc (zip/next mapped-loc)]
    (if (zip/end? next-loc)
      mapped-loc
      (recur map-fn next-loc))))

(defn- child-index [loc]
  (count (zip/lefts loc)))

(defn- distance-between [parent-loc loc]
  (count (take-while #(not= parent-loc %) (iterate zip/up loc))))

(defn- up-times [n loc]
  (nth (iterate zip/up loc) n))

(defn- remove-left-siblings [loc]
  (let [parent (zip/up loc)]
    (if (zip/lefts loc)
    (let [children (zip/children parent)]
      (zip/replace parent (hiccup/replace-children (zip/node parent) (subvec children (child-index loc)))))
    parent)))

(defn- remove-right-siblings [loc]
  (let [parent (zip/up loc)]
    (if (zip/rights loc)
      (let [children (zip/children parent)]
        (zip/replace parent (hiccup/replace-children (zip/node parent) (subvec children 0 (inc (child-index loc))))))
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

(defn- tag-path [loc]
  (map first (filter vector? (conj (zip/path loc) (zip/node loc)))))

(defn- key-of [loc]
  (hiccup/attr (zip/node loc) :key))

(defn ->caret [loc]
  (if (-> loc zip/node hiccup/text-node?)
    [(key-of (zip/up loc)) (child-index loc) 0]
    [(key-of loc) nil 0]))

