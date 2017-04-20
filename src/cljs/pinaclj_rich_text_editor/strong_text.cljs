(ns pinaclj-rich-text-editor.strong-text
  (:require
    [pinaclj-rich-text-editor.hiccup :as hiccup]
    [pinaclj-rich-text-editor.zipper :as zipper]
    [clojure.zip :as zip]))

(defn initialize [state]
  (assoc state :strong false))

(defn- insert-if-not-empty [loc insert-fn s]
  (if (= s "")
    loc
    (insert-fn loc s)))

(defn- insert-node [new-node current-text-node text-position parent-loc]
  (if (zero? (count (zip/children parent-loc)))
    (zip/insert-child parent-loc new-node)
    (let [replace-loc (nth (iterate zip/next parent-loc) (inc current-text-node))
          text (zip/node replace-loc)]
      (-> replace-loc
          (zip/replace new-node)
          (insert-if-not-empty zip/insert-left (subs text 0 text-position))
          (insert-if-not-empty zip/insert-right (subs text text-position))))))

(defn- insert-and-move-to-strong-tag [{loc :doc-loc next-key-fn :next-key-fn [_ current-text-node position] :selection-focus :as state}]
  (let [new-key (next-key-fn)
        new-node [:strong {:key new-key}]]
    (assoc state
           :doc-loc (->> loc (insert-node new-node current-text-node position))
           :selection-focus [new-key 0 0 ])))

(defn- move-right [[parent text-node _]]
  [parent (inc text-node) 0])

(defn- move-out-of-strong-tag [{loc :doc-loc focus :selection-focus :as state}]
  (let [new-loc (zip/right loc)]
    (assoc state
         :doc-loc new-loc
         :selection-focus (move-right focus))))

(defn- tag-path [loc]
  (set (map first (conj (zip/path loc) (zip/node loc)))))

(defn handle [{strong :strong loc :doc-loc :as state} _]
  (let [currently-in-tag ((tag-path loc) :strong)]
    (cond
      (and strong (not currently-in-tag))
      (insert-and-move-to-strong-tag state)
      (and (not strong) currently-in-tag)
      (move-out-of-strong-tag state)
      :else
      state)))


(defn toggle [{strong :strong :as state} key-stroke]
  (if (= key-stroke #{:B :meta})
    (assoc state :strong (not strong))
    state))
