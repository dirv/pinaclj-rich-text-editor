(ns pinaclj-rich-text-editor.toggles
  (:require
    [pinaclj-rich-text-editor.hiccup :as hiccup]
    [pinaclj-rich-text-editor.zipper :as zipper]
    [clojure.zip :as zip]))

(def toggles
  [{:key-stroke #{:B :meta} :tag :strong}
   {:key-stroke #{:I :meta} :tag :em}])

(defn initialize [state]
  (assoc state :toggles {}))

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

(defn- insert-tag [tag {loc :doc-loc next-key-fn :next-key-fn [_ _ position] :selection-focus :as state}]
  (insert-node [tag {:key (next-key-fn)}] position loc))

(defn- node-with-tag [loc tag]
  (some #(when (= tag (hiccup/tag (zip/node %))) %) (take-while #(not (nil? %)) (iterate zip/up loc))))

(defn- tags-between [parent-tag loc]
  (take-while #(not= parent-tag %) (reverse (zipper/tag-path loc))))

(defn- recreate-tree [parent-tag loc next-key-fn]
  (->> (tags-between parent-tag loc)
       hiccup/open-tags
       (hiccup/map-hiccup (partial change-key next-key-fn))))

(defn- split-tag [tag {loc :doc-loc [_ current-text-node position] :selection-focus next-key-fn :next-key-fn :as state}]
  (let [tag-node (node-with-tag loc tag)
        [left right] (zipper/split-node tag-node loc position)]
    (-> tag-node
        (#(if left (zip/insert-left % left) %))
        (zip/replace (recreate-tree tag loc next-key-fn))
        (#(if right (zip/insert-right % (hiccup/map-hiccup (partial change-key next-key-fn) right)) %))
        (zipper/find-loc hiccup/text-node?))))

(defn- update-with [state tag update-fn]
  (let [new-doc-loc (update-fn tag state)]
    (assoc state
           :doc-loc new-doc-loc
           :selection-focus (zipper/->caret new-doc-loc))))

(defn- handle-one [{toggles :toggles loc :doc-loc :as state} {tag :tag}]
  (let [toggled (get toggles tag false)
        currently-in-tag (some #{tag} (zipper/tag-path loc))]
    (cond
      (and toggled (not currently-in-tag))
      (update-with state tag insert-tag)
      (and (not toggled) currently-in-tag)
      (update-with state tag split-tag)
      :else
      state)))

(defn handle [state _]
  (reduce handle-one state toggles))

(defn- toggle-one [pressed-key state {key-stroke :key-stroke tag :tag}]
  (if (= key-stroke pressed-key)
    (assoc-in state [:toggles tag] (not (get-in state [:toggles tag])))
    state))

(defn toggle [state pressed-key]
  (reduce (partial toggle-one pressed-key) state toggles))
