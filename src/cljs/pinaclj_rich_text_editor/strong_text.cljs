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

(defn- change-key [attrs new-key-fn]
  (assoc attrs :key (new-key-fn)))

(defn- split-text [text position]
  [(subs text 0 position) (subs text position)])

(defn- do-replace [node text replace-fn]
  (if (= text "") node (replace-fn node)))

(defn- choose-first-key-or-next-key-fn [node next-key-fn]
  (let [existing-key (atom (key-of node))]
    (fn []
      (if @existing-key
        (let [this-key @existing-key]
          (reset! existing-key nil)
          this-key)
        (next-key-fn)))))

(defn- split-strong-tag [{loc :doc-loc [_ current-text-node position] :selection-focus next-key-fn :next-key-fn :as state}]
  (let [[tag attrs text] (zip/node loc)
        [left right] (split-text text position)
        next-key-fn (choose-first-key-or-next-key-fn loc next-key-fn)]
    (-> loc
        (zip/replace "")
        (do-replace left #(zip/insert-left % [tag (change-key attrs next-key-fn) left]))
        (do-replace right #(zip/insert-right % [tag (change-key attrs next-key-fn) right])))))

(defn- move-out-of-strong-tag [state]
  (let [new-loc (split-strong-tag state)]
    (assoc state
         :doc-loc new-loc
         :selection-focus (to-caret new-loc))))

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
