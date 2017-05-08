(ns pinaclj-rich-text-editor.dom-fixture)

(defn clear-dom [f]
  (let [body (.-body js/document)]
    (loop []
      (when-let [child (.-firstChild body)]
        (.removeChild body child)
        (recur))))
  (f))

(defn create-root []
  (let [root-element (.createElement js/document "div")]
    (.appendChild (.-body js/document) root-element)))

(defn find-dom-node [node-key]
  (.querySelector js/document (str "[key='" node-key "']")))

(defn ->text-node [node index]
  (nth (array-seq (.-childNodes node)) index))

(defn set-range [[start-node start-offset] [end-node end-offset]]
  (let [selection (.getSelection js/document)
        rng (.createRange js/document)]
    (.removeAllRanges selection)
    (.setStart rng start-node start-offset)
    (.setEnd rng end-node end-offset)
    (.addRange selection rng)))
