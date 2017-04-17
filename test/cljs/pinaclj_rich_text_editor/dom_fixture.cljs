(ns pinaclj-rich-text-editor.dom-fixture)

(defn clear-dom [f]
  (let [body (.-body js/document)
        firstChild (.-firstChild body)]
    (when firstChild
      (.removeChild body firstChild)))
  (f))

(defn create-root []
  (let [root-element (.createElement js/document "div")]
    (.appendChild (.-body js/document) root-element)))

(defn find-dom-node [node-key]
  (.querySelector js/document (str "[key='" node-key "']")))

(defn ->text-node [node index]
  (nth (array-seq (.-childNodes node)) index))

(defn set-range [node offset]
  (let [selection (.getSelection js/document)
        rng (.createRange js/document)]
    (.removeAllRanges selection)
    (.setStart rng node offset)
    (.collapse rng true)
    (.addRange selection rng)))
