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
    (zip/down (zip/insert-child parent-loc new-node))
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

(defn- text-node-index [text-node-loc]
  (count (zip/lefts text-node-loc)))

(defn- key-of [loc]
  (hiccup/attr (zip/node loc) :key))

(defn- text-node? [loc]
  (string? (zip/node loc)))

(defn- to-caret [loc]
  (if (text-node? loc)
    [(key-of (zip/up loc)) (text-node-index loc) 0]
    [(key-of loc) 0 0]))

(defn- cut-at
  ([[tag attrs text-node] start end]
   [tag attrs (subs text-node start end)])
  ([[tag attrs text-node] start]
   [tag attrs (subs text-node start)]))

(defn- tag-path [loc]
  (map first (conj (zip/path loc) (zip/node loc))))

(defn- child-node-of-tag [loc parent-tag]
  (last (take-while #(and (not (nil? %)) (not= parent-tag (hiccup/tag (zip/node %)))) (iterate zip/up loc))))

(defn- node-with-tag [loc tag]
  (some #(when (= tag (hiccup/tag (zip/node %))) %) (take-while #(not (nil? %)) (iterate zip/up loc))))

(defn- change-key [key-fn node]
  (if (vector? node)
    (hiccup/set-attr node :key (if key-fn (key-fn) 2))
    node))


(defn- tags-between [parent-tag loc]
  (println "parent tag" parent-tag (reverse (tag-path loc)))
  (take-while #(not= parent-tag %) (reverse (tag-path loc))))

(defn- open-tags [[& tags]]
  (if (seq? tags)
    [(first tags) (open-tags (rest tags))]
    ""))

(defn- split-strong-tag [{loc :doc-loc [_ current-text-node position] :selection-focus next-key-fn :next-key-fn :as state}]
  (let [replace-loc (nth (iterate zip/next loc) (inc current-text-node))
        strong-node (node-with-tag loc :strong)
        tags-between (tags-between :strong loc)
        [left right] (zipper/split-node strong-node replace-loc position)]
    (-> strong-node
        (#(if left (zip/insert-left % left) %))
        (zip/replace (hiccup/map-hiccup (partial change-key next-key-fn) (open-tags tags-between)))
        (#(if right (zip/insert-right % (hiccup/map-hiccup (partial change-key next-key-fn) right)) %)))))

(defn- move-out-of-strong-tag [state]
  (let [new-loc (split-strong-tag state)]
    (assoc state
         :doc-loc new-loc
         :selection-focus (to-caret new-loc))))

(defn handle [{strong :strong loc :doc-loc :as state} _]
  (let [currently-in-tag (some #{:strong} (tag-path loc))]
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
