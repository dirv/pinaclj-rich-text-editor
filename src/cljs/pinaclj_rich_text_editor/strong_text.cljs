(ns pinaclj-rich-text-editor.strong-text
  (:require
    [pinaclj-rich-text-editor.hiccup :as hiccup]
    [pinaclj-rich-text-editor.zipper :as zipper]
    [clojure.zip :as zip]))

(defn initialize [state]
  (assoc state :strong false))

(defn- if-not-empty [loc insert-fn s]
  (if (= s "")
    loc
    (insert-fn loc s)))

(defn- insert-node [new-node text-position replace-loc]
  (if (hiccup/text-node? (zip/node replace-loc))
    (let [text (zip/node replace-loc)]
      (-> replace-loc
          (zip/replace new-node)
          (if-not-empty zip/insert-left (subs text 0 text-position))
          (if-not-empty zip/insert-right (subs text text-position))))
    (zip/down (zip/insert-child replace-loc new-node))))

(defn- change-key [key-fn node]
  (if (hiccup/text-node? node)
    node
    (hiccup/set-attr node :key (if key-fn (key-fn) 2))))

(defn- insert-strong-tag [{loc :doc-loc next-key-fn :next-key-fn [_ _ position] :selection-focus :as state}]
  (insert-node [:strong {:key (next-key-fn)}] position loc))

(defn- node-with-tag [loc tag]
  (some #(when (= tag (hiccup/tag (zip/node %))) %) (take-while #(not (nil? %)) (iterate zip/up loc))))

(defn- tags-between [parent-tag loc]
  (take-while #(not= parent-tag %) (reverse (zipper/tag-path loc))))

(defn- recreate-tree [parent-tag loc next-key-fn]
  (->> (tags-between parent-tag loc)
       hiccup/open-tags
       (hiccup/map-hiccup (partial change-key next-key-fn))))

(defn- split-strong-tag [{loc :doc-loc [_ current-text-node position] :selection-focus next-key-fn :next-key-fn :as state}]
  (let [strong-node (node-with-tag loc :strong)
        [left right] (zipper/split-node strong-node loc position)]
    (-> strong-node
        (#(if left (zip/insert-left % left) %))
        (zip/replace (recreate-tree :strong loc next-key-fn))
        (#(if right (zip/insert-right % (hiccup/map-hiccup (partial change-key next-key-fn) right)) %))
        (zipper/find-loc hiccup/text-node?))))

(defn- update-with [state update-fn]
  (let [new-doc-loc (update-fn state)]
    (assoc state
           :doc-loc new-doc-loc
           :selection-focus (zipper/->caret new-doc-loc))))

(defn handle [{strong :strong loc :doc-loc :as state} _]
  (let [currently-in-tag (some #{:strong} (zipper/tag-path loc))]
    (cond
      (and strong (not currently-in-tag))
      (update-with state insert-strong-tag)
      (and (not strong) currently-in-tag)
      (update-with state split-strong-tag)
      :else
      state)))

(defn toggle [{strong :strong :as state} key-stroke]
  (if (= key-stroke #{:B :meta})
    (assoc state :strong (not strong))
    state))
